package unit731.hunlinter.gui;

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
