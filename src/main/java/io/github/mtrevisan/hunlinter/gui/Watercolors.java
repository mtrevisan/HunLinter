/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.gui;

import java.awt.Color;


/** @see <a href="http://www.easyrgb.com/en/create.php">Watercolors</a> */
public enum Watercolors{
	//http://www.color-hex.com/
	//http://fabrizio.zellini.org/tabella-nomi-e-codici-html-rgb-dei-colori
	SHELL("#FFF5EE"),
	ANTI_FLASH_WHITE("#F2F3F4"),
	SUGAR_PAPER("#E0FFFF"),
	CREAM("#FFFDD0"),
	ROSE_LAVANDER("#FFF0F5"),
	ISABELLA("#F4F0EC"),
	ALICE_BLUE("#F0F8FF"),
	MAGNOLIA("#F8F4FF"),
	BEIGE("#F5F5DC"),
	LINEN("#FAF0E6"),
	GREEN_TEA("#D0F0C0");


	private final Color color;


	Watercolors(final String hexColor){
		color = Color.decode(hexColor);
	}

	public Color getColor(){
		return color;
	}

}
