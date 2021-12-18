package cz.vutbr.web.csskit.antlr;

import java.net.URL;
import java.nio.charset.Charset;

import org.w3c.dom.Element;

/**
 * Wrapper for CSS source content.
 *
 * @author bertfrees
 */
public class CSSSource {

	public static enum SourceType {
		INLINE,
		EMBEDDED,
		URL
	}

	/**
	 * The type of source: inline, embedded or URL.
	 */
	public SourceType type;

	/**
	 * The content: raw data (String) or URL.
	 */
	public Object source;

	/**
	 * Element that inline style is attached to, or null if type is not <code>INLINE</code>.
	 */
	public Element inlineElement;

	/**
	 * Byte encoding of content at URL, or <code>null</code> if type is not URL or if the encoding
	 * is not known.
	 */
	public Charset encoding;

	/**
	 * Base URL for resolving relative URLs against.
	 */
	public URL base;

	/**
	 * Line number of the first character.
	 */
	public int lineOffset = 0;

	/**
	 * Column number of the first character.
	 */
	public int columnOffset = 0;

	/**
	 * The media type as specified on the <code>style</code> or <code>link</code> element
	 * ("text/css", "text/x-scss", ...), or <code>null</code> if not specified.
	 */
	public String mediaType;

	public CSSSource(String source, Element inlineElement, URL base) {
		this.type = SourceType.INLINE;
		this.source = source;
		this.inlineElement = inlineElement;
		this.base = base;
	}

	public CSSSource(String source, String mediaType, URL base, int lineOffset, int columnOffset) {
		this.type = SourceType.EMBEDDED;
		this.source = source;
		this.mediaType = mediaType;
		this.base = base;
		if (lineOffset > 0)
			this.lineOffset = lineOffset;
		if (columnOffset > 0)
			this.columnOffset = columnOffset;
	}

	public CSSSource(URL source, Charset encoding, String mediaType) {
		this.type = SourceType.URL;
		this.source = source;
		this.encoding = encoding;
		this.mediaType = mediaType;
		this.base = source;
	}
}
