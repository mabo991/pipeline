package org.daisy.pipeline.tts.google.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.daisy.common.file.URLs;
import org.daisy.pipeline.tts.TTSEngine;
import org.daisy.pipeline.tts.TTSRegistry.TTSResource;
import org.daisy.pipeline.tts.TTSService.SynthesisException;
import org.daisy.pipeline.tts.Voice;
import org.daisy.pipeline.tts.VoiceInfo;
import org.daisy.pipeline.tts.VoiceInfo.Gender;
import org.daisy.pipeline.tts.rest.Request;
import org.daisy.pipeline.tts.scheduler.ExponentialBackoffScheduler;
import org.daisy.pipeline.tts.scheduler.FatalError;
import org.daisy.pipeline.tts.scheduler.RecoverableError;
import org.daisy.pipeline.tts.scheduler.Schedulable;
import org.daisy.pipeline.tts.scheduler.Scheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector class to synthesize audio using the google cloud tts engine.
 *
 * <p>This connector is based on their REST Api.</p>
 *
 * @author Louis Caille @ braillenet.org
 */
public class GoogleRestTTSEngine extends TTSEngine {

	private final AudioFormat mAudioFormat;
	private final Scheduler<Schedulable> mRequestScheduler;
	private final int mPriority;
	private final GoogleRequestBuilder mRequestBuilder;

	private static final URL ssmlTransformer = URLs.getResourceFromJAR("/transform-ssml.xsl", GoogleRestTTSEngine.class);
	private static final Logger logger = LoggerFactory.getLogger(GoogleRestTTSEngine.class);

	public GoogleRestTTSEngine(GoogleTTSService googleService, String serverAddress, String apiKey, AudioFormat audioFormat, int priority) {
		super(googleService);
		mPriority = priority;
		mAudioFormat = audioFormat;
		mRequestScheduler = new ExponentialBackoffScheduler<Schedulable>();
		mRequestBuilder = new GoogleRequestBuilder(serverAddress, apiKey);
	}

	@Override
	public SynthesisResult synthesize(XdmNode ssml, Voice voice, TTSResource threadResources)
			throws SynthesisException, InterruptedException {
	
		String sentence; {
			Map<String,Object> xsltParams = new HashMap<>(); {
				if (voice != null) xsltParams.put("voice", voice.name);
			}
			try {
				sentence = transformSsmlNodeToString(ssml, ssmlTransformer, xsltParams);
			} catch (IOException | SaxonApiException e) {
				throw new SynthesisException(e);
			}
		}

		if (sentence.length() > 5000) {
			throw new SynthesisException("The number of characters in the sentence must not exceed 5000.");
		}

		String languageCode;
		String name;

		if (voice != null) {
			name = voice.name;
			languageCode = voice.getLocale().get().toLanguageTag(); // assume locale is declared
		} else {
			// by default the voice is set to English
			languageCode = "en-GB";
			name = "en-GB-Standard-A";
		}

		try {
			Request<JSONObject> speechRequest = mRequestBuilder.newRequest()
				.withSampleRate((int)mAudioFormat.getSampleRate())
				.withAction(GoogleRestAction.SPEECH)
				.withLanguageCode(languageCode)
				.withVoice(name)
				.withText(sentence)
				.build();
			ArrayList<byte[]> result = new ArrayList<>();
			mRequestScheduler.launch(() -> {
				Response response = doRequest(speechRequest);
				if (response.status == 429)
					throw new RecoverableError("Exceeded quotas", response.exception);
				else if (response.status != 200)
					throw new FatalError("Response code " + response.status, response.exception);
				else if (response.body == null)
					throw new FatalError("Response body is null", response.exception);
				String json; {
					try {
						json = readStream(response.body);
					} catch (IOException  e) {
						throw new FatalError(e);
					}
				}
				try {
					// assume response is JSON object with single "audioContent" string
					String audioContent = new JSONObject(json).getString("audioContent");
					// the answer is encoded in base 64
					byte[] decodedBytes = Base64.getDecoder().decode(audioContent);
					result.add(decodedBytes);
				} catch (JSONException e) {
					throw new FatalError("JSON could not be parsed:\n" + json, e);
				}
			});
			return new SynthesisResult(createAudioStream(mAudioFormat, result.get(0)));
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			if (e instanceof FatalError)
				throw new SynthesisException(e.getMessage(), e.getCause());
			else
				throw new SynthesisException(e);
		}
	}

	@Override
	public Collection<Voice> getAvailableVoices() throws SynthesisException, InterruptedException {
		Collection<Voice> result = new ArrayList<Voice>();
		try {
			Request<JSONObject> voicesRequest = mRequestBuilder.newRequest()
				.withAction(GoogleRestAction.VOICES)
				.build();
			mRequestScheduler.launch(() -> {
				Response response = doRequest(voicesRequest);
				if (response.status == 429)
					throw new RecoverableError("Exceeded quotas", response.exception);
				else if (response.status != 200)
					throw new FatalError("Response code " + response.status, response.exception);
				else if (response.body == null)
					throw new FatalError("Response body is null", response.exception);
				String json; {
					try {
						json = readStream(response.body);
					} catch (IOException  e) {
						throw new FatalError(e);
					}
				}
				try {
					// assume response is JSON object with single "voices" array
					JSONArray voices = new JSONObject(json).getJSONArray("voices");
					for (int i = 0; i < voices.length(); i++) {
						// assume elements are objects
						JSONObject voice = voices.getJSONObject(i);
						// assume "name" string is present
						String name = voice.getString("name");
						Gender gender; {
							// assume "ssmlGender" string is present
							String g = voice.getString("ssmlGender");
							if ("SSML_VOICE_GENDER_UNSPECIFIED".equals(g))
								gender = Gender.ANY;
							else {
								gender = Gender.of(g);
								if (gender == null)
									logger.debug("Could not parse gender: " + g);
							}
						}
						if (gender != null) {
							// assume "languageCodes" array is present and is not empty
							// only take the language that is listed first
							String locale = voice.getJSONArray("languageCodes").getString(0);
							try {
								result.add(new Voice(getProvider().getName(), name, VoiceInfo.tagToLocale(locale), gender));
							} catch (VoiceInfo.UnknownLanguage e) {
								logger.debug("Could not parse locale: " + locale);
							}
						}
					}
				} catch (JSONException e) {
					throw new FatalError("Voices list could not be parsed:\n" + json, e);
				}
			});
		} catch (InterruptedException e) {
			throw e;
		} catch (FatalError e) {
			throw new SynthesisException(e.getMessage(), e.getCause());
		} catch (Exception e) {
			throw new SynthesisException(e);
		}
		return result;
	}

	@Override
	public int getOverallPriority() {
		return mPriority;
	}

	@Override
	public TTSResource allocateThreadResources() throws SynthesisException,
	InterruptedException {
		return new TTSResource();
	}

	private static class Response {
		int status;
		InputStream body;
		IOException exception;
	}

	/**
	 * Send request and get response status code and body.
	 *
	 * <p><code>body</code> is null if <code>exception</code> is non-null and visa-versa.</p>
	 *
	 * @throws FatalError if <code>status</code> can not be retrieved.
	 */
	private static Response doRequest(Request request) throws InterruptedException, FatalError {
		Response r = new Response();
		try {
			r.body = request.send();
		} catch (IOException e) {
			r.exception = e;
		}
		try {
			r.status = request.getConnection().getResponseCode();
		} catch (IOException responseCodeError) {
			throw new FatalError("could not retrieve response code for request", responseCodeError);
		}
		return r;
	}

	/**
	 * Read InputStream as a UTF-8 encoded string.
	 */
	private static String readStream(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line.trim());
		}
		br.close();
		return sb.toString();
	}
}
