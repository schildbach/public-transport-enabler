/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;

import okhttp3.HttpUrl;

/**
 * @author Andreas Schildbach
 */
public class VblProvider extends AbstractEfaProvider {
    private static final HttpUrl API_BASE = HttpUrl.parse("http://mobil.vbl.ch/vblmobil/");

    public VblProvider() {
        super(NetworkId.VBL, API_BASE);
        setRequestUrlEncoding(Charsets.UTF_8);
        setUseRouteIndexAsTripId(false);
    }

    @Override
    protected Line parseLine(final @Nullable String id, final @Nullable String network, final @Nullable String mot,
            final @Nullable String symbol, final @Nullable String name, final @Nullable String longName,
            final @Nullable String trainType, final @Nullable String trainNum, final @Nullable String trainName) {
        if ("0".equals(mot)) {
            if ("BLS".equals(trainType) && trainNum != null)
                return new Line(id, network, Product.REGIONAL_TRAIN, "BLS" + trainNum);
            if ("ASM".equals(trainType) && trainNum != null) // Aare Seeland mobil
                return new Line(id, network, Product.REGIONAL_TRAIN, "ASM" + trainNum);
            if ("SOB".equals(trainType) && trainNum != null) // Schweizerische Südostbahn
                return new Line(id, network, Product.REGIONAL_TRAIN, "SOB" + trainNum);
            if ("RhB".equals(trainType) && trainNum != null) // Rhätische Bahn
                return new Line(id, network, Product.REGIONAL_TRAIN, "RhB" + trainNum);
            if ("AB-".equals(trainType) && trainNum != null) // Appenzeller Bahnen
                return new Line(id, network, Product.REGIONAL_TRAIN, "AB" + trainNum);
            if ("BDW".equals(trainType) && trainNum != null) // BDWM Transport
                return new Line(id, network, Product.REGIONAL_TRAIN, "BDW" + trainNum);
            if ("ZB".equals(trainType) && trainNum != null) // Zentralbahn
                return new Line(id, network, Product.REGIONAL_TRAIN, "ZB" + trainNum);
            if ("TPF".equals(trainType) && trainNum != null) // Transports publics fribourgeois
                return new Line(id, network, Product.REGIONAL_TRAIN, "TPF" + trainNum);
            if ("MGB".equals(trainType) && trainNum != null) // Matterhorn Gotthard Bahn
                return new Line(id, network, Product.REGIONAL_TRAIN, "MGB" + trainNum);
            if ("CJ".equals(trainType) && trainNum != null) // Chemins de fer du Jura
                return new Line(id, network, Product.REGIONAL_TRAIN, "CJ" + trainNum);
            if ("LEB".equals(trainType) && trainNum != null) // Lausanne-Echallens-Bercher
                return new Line(id, network, Product.REGIONAL_TRAIN, "LEB" + trainNum);
            if ("FAR".equals(trainType) && trainNum != null) // Ferrovie Autolinee Regionali Ticinesi
                return new Line(id, network, Product.REGIONAL_TRAIN, "FAR" + trainNum);
            if ("WAB".equals(trainType) && trainNum != null) // Wengernalpbahn
                return new Line(id, network, Product.REGIONAL_TRAIN, "WAB" + trainNum);
            if ("JB".equals(trainType) && trainNum != null) // Jungfraubahn
                return new Line(id, network, Product.REGIONAL_TRAIN, "JB" + trainNum);
            if ("NSt".equals(trainType) && trainNum != null) // Nyon-St-Cergue-Morez
                return new Line(id, network, Product.REGIONAL_TRAIN, "NSt" + trainNum);
            if ("RA".equals(trainType) && trainNum != null) // Regionalps
                return new Line(id, network, Product.REGIONAL_TRAIN, "RA" + trainNum);
            if ("TRN".equals(trainType) && trainNum != null) // Transport Publics Neuchâtelois
                return new Line(id, network, Product.REGIONAL_TRAIN, "TRN" + trainNum);
            if ("TPC".equals(trainType) && trainNum != null) // Transports Publics du Chablais
                return new Line(id, network, Product.REGIONAL_TRAIN, "TPC" + trainNum);
            if ("MVR".equals(trainType) && trainNum != null) // Montreux-Vevey-Riviera
                return new Line(id, network, Product.REGIONAL_TRAIN, "MVR" + trainNum);
            if ("MOB".equals(trainType) && trainNum != null) // Montreux-Oberland Bernois
                return new Line(id, network, Product.REGIONAL_TRAIN, "MOB" + trainNum);
            if ("TRA".equals(trainType) && trainNum != null) // Transports Vallée de Joux-Yverdon-Ste-Croix
                return new Line(id, network, Product.REGIONAL_TRAIN, "TRA" + trainNum);
            if ("TMR".equals(trainType) && trainNum != null) // Transports de Martigny et Régions
                return new Line(id, network, Product.REGIONAL_TRAIN, "TMR" + trainNum);
            if ("GGB".equals(trainType) && trainNum != null) // Gornergratbahn
                return new Line(id, network, Product.REGIONAL_TRAIN, "GGB" + trainNum);
            if ("BLM".equals(trainType) && trainNum != null) // Lauterbrunnen-Mürren
                return new Line(id, network, Product.REGIONAL_TRAIN, "BLM" + trainNum);
        }

        return super.parseLine(id, network, mot, symbol, name, longName, trainType, trainNum, trainName);
    }
}
