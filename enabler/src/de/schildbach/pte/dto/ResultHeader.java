/*
 * Copyright 2010-2015 the original author or authors.
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

package de.schildbach.pte.dto;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

import de.schildbach.pte.NetworkId;

/**
 * @author Andreas Schildbach
 */
@SuppressWarnings("serial")
public final class ResultHeader implements Serializable {
    public final NetworkId network;
    public final String serverProduct;
    public final @Nullable String serverVersion;
    public final @Nullable String serverName;
    public final long serverTime;
    public final Object context;

    public ResultHeader(final NetworkId network, final String serverProduct) {
        this(network, serverProduct, null, null, 0, null);
    }

    public ResultHeader(final NetworkId network, final String serverProduct, final String serverVersion,
            final String serverName, final long serverTime, final Object context) {
        this.network = checkNotNull(network);
        this.serverProduct = checkNotNull(serverProduct);
        this.serverVersion = serverVersion;
        this.serverName = serverName;
        this.serverTime = serverTime;
        this.context = context;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("serverProduct", serverProduct).add("serverVersion", serverVersion)
                .add("serverName", serverName).add("serverTime", serverTime).add("context", context).omitNullValues()
                .toString();
    }
}
