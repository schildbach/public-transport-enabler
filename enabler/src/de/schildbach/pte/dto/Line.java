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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.dto;

import java.io.Serializable;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * @author Andreas Schildbach
 */
public final class Line implements Serializable, Comparable<Line>
{
	public enum Attr
	{
		CIRCLE_CLOCKWISE, CIRCLE_ANTICLOCKWISE, SERVICE_REPLACEMENT, LINE_AIRPORT, WHEEL_CHAIR_ACCESS, BICYCLE_CARRIAGE
	}

	private static final long serialVersionUID = -5642533805998375070L;

	public final String id;
	public final Product product;
	public final String label;
	public final Style style;
	public final Set<Attr> attrs;
	public final String message;

	public static final Line FOOTWAY = new Line(null, null, null);
	public static final Line TRANSFER = new Line(null, null, null);
	public static final Line SECURE_CONNECTION = new Line(null, null, null);
	public static final Line DO_NOT_CHANGE = new Line(null, null, null);

	public Line(final String id, final Product product, final String label)
	{
		this(id, product, label, null, null, null);
	}

	public Line(final String id, final Product product, final String label, final Style style)
	{
		this(id, product, label, style, null, null);
	}

	public Line(final String id, final Product product, final String label, final Style style, final String message)
	{
		this(id, product, label, style, null, message);
	}

	public Line(final String id, final Product product, final String label, final Style style, final Set<Attr> attrs)
	{
		this(id, product, label, style, attrs, null);
	}

	public Line(final String id, final Product product, final String label, final Style style, final Set<Attr> attrs, final String message)
	{
		this.id = id;
		this.product = product;
		this.label = label;
		this.style = style;
		this.attrs = attrs;
		this.message = message;
	}

	public char productCode()
	{
		return product != null ? product.code : Product.UNKNOWN;
	}

	public boolean hasAttr(final Attr attr)
	{
		return attrs != null && attrs.contains(attr);
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Line))
			return false;
		final Line other = (Line) o;
		if (!Objects.equal(this.product, other.product))
			return false;
		return Objects.equal(this.label, other.label);
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(product, label);
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this) //
				.addValue(product) //
				.addValue(label) //
				.toString();
	}

	public int compareTo(final Line other)
	{
		return ComparisonChain.start() //
				.compare(this.product, other.product, Ordering.natural().nullsLast()) //
				.compare(this.label, other.label, Ordering.natural().nullsLast()) //
				.result();
	}
}
