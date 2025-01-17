package org.daisy.pipeline.audio.lame.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.daisy.common.shell.CommandRunner;
import org.daisy.common.shell.CommandRunner.Consumer;
import org.daisy.pipeline.audio.AudioEncoder;
import static org.daisy.pipeline.audio.AudioFileTypes.MP3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LameEncoder implements AudioEncoder {

	static class LameEncodingOptions {
		String binpath;
		Integer bitrate;
		String[] extraCliArguments;
	}

	private static final Logger mLogger = LoggerFactory.getLogger(LameEncoder.class);

	private final LameEncodingOptions lameOpts;

	LameEncoder(LameEncodingOptions lameOpts) {
		this.lameOpts = lameOpts;
	}

	@Override
	public void encode(AudioInputStream pcm, AudioFileFormat.Type outputFileType, File outputFile) throws Throwable {
		if (!MP3.equals(outputFileType))
			throw new IllegalArgumentException();

		AudioFormat audioFormat = pcm.getFormat();
		String freq = String.valueOf((Float.valueOf(audioFormat.getSampleRate()) / 1000));
		String bitwidth = String.valueOf(audioFormat.getSampleSizeInBits());
		String signedOpt = audioFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED ? "--unsigned"
		        : "--signed";
		String endianness = audioFormat.isBigEndian() ? "--big-endian" : "--little-endian";
		Consumer<OutputStream> lameInput;

		if (!(audioFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED
		      || audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
		      || audioFormat.getEncoding() == AudioFormat.Encoding.PCM_FLOAT)) {
			throw new RuntimeException("Expected PCM encoded audio");
		}

		// Lame cannot deal with unsigned encoding for other bitwidths than 8
		if (audioFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED
		    && audioFormat.getSampleSizeInBits() > 8) {
			if ((audioFormat.getSampleSizeInBits() % 8) != 0) {
				throw new RuntimeException("Expected a bitwidth that is a multiple of 8");
			}
			// downsampling: keep the most significant bit only, in order to produce 8-bit unsigned data
			int ratio = audioFormat.getSampleSizeInBits() / 8;
			int mse = audioFormat.isBigEndian() ? 0 : (ratio - 1);
			bitwidth = "8";
			lameInput = stream -> {
				try (BufferedOutputStream out = new BufferedOutputStream(stream)) {
					byte[] frame = new byte[audioFormat.getFrameSize()];
					while (pcm.read(frame) > 0)
						for (int i = 0; i < frame.length; i += ratio)
							out.write(frame[i + mse]);
				}
			};
		} else if (audioFormat.getEncoding() == AudioFormat.Encoding.PCM_FLOAT) {
			// convert [-1.0, 1.0] values to regular 32-bit signed integers
			// FIXME: find a faster and more accurate way
			switch (audioFormat.getSampleSizeInBits()) {
			case 32:
				lameInput = stream -> {
					try (BufferedOutputStream out = new BufferedOutputStream(stream)) {
						ByteBuffer frame = ByteBuffer.wrap(new byte[audioFormat.getFrameSize()]);
						ByteOrder byteOrder = audioFormat.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
						while (pcm.read(frame.array()) > 0) {
							frame.order(byteOrder);
							// read floats and write ints (both 4 bytes)
							for (int i = 0; i < frame.array().length; i += 4)
								frame.putInt(0, (int)(frame.getFloat(i) * Integer.MAX_VALUE));
							out.write(frame.array(), 0, 4);
						}
					}
				};
				break;
			case 64:
				// Lame cannot handle 64-bit data => downsampling to 32-bit
				bitwidth = "32";
				lameInput = stream -> {
					try (BufferedOutputStream out = new BufferedOutputStream(stream)) {
						ByteBuffer frame = ByteBuffer.wrap(new byte[audioFormat.getFrameSize()]);
						ByteOrder byteOrder = audioFormat.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
						while (pcm.read(frame.array()) > 0) {
							frame.order(byteOrder);
							// read doubles (8 bytes) and write ints (4 bytes)
							for (int i = 0; i < frame.array().length; i += 8)
								frame.putInt(0, (int)(frame.getDouble(i) * Integer.MAX_VALUE));
							out.write(frame.array(), 0, 4);
						}
					}
				};
				break;
			default:
				throw new RuntimeException("Expected either 32- or 64-bit samples");
			}
		} else {
			lameInput = stream -> {
				try (BufferedOutputStream out = new BufferedOutputStream(stream)) {
					byte[] frame = new byte[audioFormat.getFrameSize()];
					while (pcm.read(frame) > 0)
						out.write(frame);
				}
			};
		}

		List<String> cmd = new ArrayList<>(); {
			cmd.add(lameOpts.binpath);
			// input options
			cmd.add("-r");         // raw PCM
			cmd.add("-s");         // sample rate (kHz)
			cmd.add(freq);
			cmd.add("--bitwidth"); // bits per sample
			cmd.add(bitwidth);
			cmd.add(signedOpt);    // --unsigned | --signed
			cmd.add(endianness);   // --big-endian | --little-endian";
			cmd.add("-m");         // mode
			cmd.add("m");          // mono
			// output options
			if (lameOpts.bitrate != null) {
				cmd.add("-b");     // minimum bitrate to be used
				cmd.add("" + lameOpts.bitrate);
			}
			if (lameOpts.extraCliArguments != null)
				for (String arg : lameOpts.extraCliArguments)
					cmd.add(arg);
			// verbosity
			cmd.add("--silent");
			cmd.add("-");          // read from stdin
			cmd.add(outputFile.getAbsolutePath());
		}
		new CommandRunner(cmd.toArray(new String[cmd.size()]))
			.feedInput(lameInput)
			.consumeError(mLogger)
			.run();
	}
}
