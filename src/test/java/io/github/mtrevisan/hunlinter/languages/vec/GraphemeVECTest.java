/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.languages.vec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class GraphemeVECTest{

	@Test
	void dyphtongStrong(){
		//àa, aa
		Assertions.assertTrue(GraphemeVEC.isDiphtong("buxaràa"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("fiàa"));
		//àe, ae
		Assertions.assertTrue(GraphemeVEC.isDiphtong("fràe"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("poràe"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("guàena"));
		//ào, ao
		Assertions.assertTrue(GraphemeVEC.isDiphtong("masañào"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("rào"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("tàol"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("fàola"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("kaorèr"));

		//éa, ea
		Assertions.assertTrue(GraphemeVEC.isDiphtong("kavixéa"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("béar"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("féara"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("kréa"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("kreador"));
		//ée, ee
		Assertions.assertTrue(GraphemeVEC.isDiphtong("sée"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("léexe"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("àmee"));
		//éo, eo
		Assertions.assertTrue(GraphemeVEC.isDiphtong("menadéo"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("jéor"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("néola"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("spéo"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("idòneo"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("teoría"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("reoxamente"));

		//èa
		Assertions.assertTrue(GraphemeVEC.isDiphtong("borèa"));
		//èe
		Assertions.assertTrue(GraphemeVEC.isDiphtong("pièe"));
		//èo
		Assertions.assertTrue(GraphemeVEC.isDiphtong("skèo"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("rèola"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("sièol"));

		//òo, óo
		Assertions.assertTrue(GraphemeVEC.isDiphtong("đòo"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("đóo"));
	}

	@Test
	void dyphtongWeak(){
		//ía
		Assertions.assertTrue(GraphemeVEC.isDiphtong("poría"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("kualsíasi"));
		//íe
		Assertions.assertTrue(GraphemeVEC.isDiphtong("píe"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("poríelo"));
		//íi
		Assertions.assertTrue(GraphemeVEC.isDiphtong("defeníi"));
		//ío
		Assertions.assertTrue(GraphemeVEC.isDiphtong("jïografía"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("dexvío"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("víolo"));
		//íu
		Assertions.assertTrue(GraphemeVEC.isDiphtong("pipíu"));

		//úa
		Assertions.assertTrue(GraphemeVEC.isDiphtong("gúa"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("menúa"));
		//úe
		Assertions.assertTrue(GraphemeVEC.isDiphtong("gúe"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("menúe"));
		//úi
		Assertions.assertTrue(GraphemeVEC.isDiphtong("agúi"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("súito"));
		//úo
		Assertions.assertTrue(GraphemeVEC.isDiphtong("verúo"));
		Assertions.assertTrue(GraphemeVEC.isDiphtong("túoe"));
	}

	@Test
	void hyatus(){
		//aa, aà
		//ae, aè, aé
		Assertions.assertTrue(GraphemeVEC.isHyatus("maestro"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("saeta"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("inkaécoe"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("maèstro"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("fraèl"));
		//ai, aí
		Assertions.assertTrue(GraphemeVEC.isHyatus("baíxiko"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("petaiŧa"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("rai"));
		//ao, aò, aó
		Assertions.assertTrue(GraphemeVEC.isHyatus("xdrao"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("diaoleto"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("straóltoe"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("saòn"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("mariaòrbola"));
		//au, aú
		Assertions.assertTrue(GraphemeVEC.isHyatus("paura"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("fraúskola"));

		//ea, eà
		Assertions.assertTrue(GraphemeVEC.isHyatus("leàtego"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("kreanŧa"));
		//ee, eè, eé
		Assertions.assertTrue(GraphemeVEC.isHyatus("kaveel"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("kreenŧa"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("meexina"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("peerada"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("greèla"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("veèl"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("treèximo"));
		//ei, eí
		Assertions.assertTrue(GraphemeVEC.isHyatus("deítego"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("reseidor"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("reina"));
		//eo, eò, eó
		Assertions.assertTrue(GraphemeVEC.isHyatus("dreo"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("alveolar"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("neóo"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("peònia"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("peòco"));
		//eu, eú
		Assertions.assertTrue(GraphemeVEC.isHyatus("eurixma"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("reúŧol"));

		//ïa
		Assertions.assertTrue(GraphemeVEC.isHyatus("ovïar"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("jïaspro"));
		//ïe, ïè, ïé
		Assertions.assertTrue(GraphemeVEC.isHyatus("rïesir"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("rïèser"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("rïéser"));
		//ïí, ïi
		Assertions.assertTrue(GraphemeVEC.isHyatus("xvïi"));
		//ïo
		Assertions.assertTrue(GraphemeVEC.isHyatus("pïolar"));
		//ïu, ïú
		Assertions.assertTrue(GraphemeVEC.isHyatus("kïuo"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("rïúsoe"));
		//iú
		Assertions.assertTrue(GraphemeVEC.isHyatus("piúbiko"));


		//oa, oà
		Assertions.assertTrue(GraphemeVEC.isHyatus("roao"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("koabitar"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("koàbito"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("kroà"));
		//oe, oé, oè
		Assertions.assertTrue(GraphemeVEC.isHyatus("koertor"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("Soérđen"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("soèntro"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("ŧoè"));
		//oi, oí
		Assertions.assertTrue(GraphemeVEC.isHyatus("foina"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("roínoe"));
		//oo, oò, oó
		Assertions.assertTrue(GraphemeVEC.isHyatus("koordenativo"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("koórdeno"));
		//oú

		//üa, üà
		Assertions.assertTrue(GraphemeVEC.isHyatus("sküaena"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("destretüal"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("jexüato"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("stüa"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("rexidüar"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("spüàcoe"));
		//üe, üè, üé
		Assertions.assertTrue(GraphemeVEC.isHyatus("sküena"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("stüe"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("ŧüetar"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("ŧüétoe"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("minüèl"));
		//üi, üí, uí
		Assertions.assertTrue(GraphemeVEC.isHyatus("rüina"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("argüir"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("spüi"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("püin"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("cüí"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("küía"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("guíndol"));
		//üo, üò
		Assertions.assertTrue(GraphemeVEC.isHyatus("flüòr"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("esètüo"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("tüon"));
		Assertions.assertTrue(GraphemeVEC.isHyatus("spüo"));
	}

}
