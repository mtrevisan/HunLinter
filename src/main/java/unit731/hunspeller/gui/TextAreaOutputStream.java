package unit731.hunspeller.gui;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.swing.JTextArea;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class TextAreaOutputStream extends OutputStream{

	private static final byte[] SUPPORT = new byte[6];

	private final JTextArea textArea;


	@Override
	public void write(int codepoint) throws IOException{
		//redirects data to the text area
		String converted = new String(utf8(codepoint), StandardCharsets.UTF_8);
		textArea.append(converted);
		//crolls the text area to the end of data
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	private static byte[] utf8(int codepoint){
		int index = 1;
		codepoint &= 0xFF;
		if(codepoint < 0x80)
			SUPPORT[0] = (byte)codepoint;
		else{
			index = 0;
			int lastPrefix = 0xC0;
			while(true){
				SUPPORT[index ++] = (byte)(0x80 | (codepoint & 0x3F));

				codepoint >>= 6;
				if((codepoint & 0xE0) == 0){
					SUPPORT[index ++] = (byte)(lastPrefix | codepoint);

					break;
				}

				lastPrefix = (0x80 | (lastPrefix >> 1)) >> 1;
			}

			reverse(SUPPORT, index);
		}
		if(index < SUPPORT.length)
			SUPPORT[index] = 0x00;

		return SUPPORT;
	}

	/**
	 * Reverses the order of the given array in the given range starting from {@code 0}.
	 *
	 * @param array	the array to reverse, must NOT be {@code null}
	 * @param endIndexExclusive	elements up to {@code endIndex - 1} are reversed in the array
	 */
	public static void reverse(byte[] array, int endIndexExclusive){
		int i = 0;
		int j = endIndexExclusive - 1;
		while(j > i){
			byte tmp = array[j];
			array[j --] = array[i];
			array[i ++] = tmp;
		}
	}

}
