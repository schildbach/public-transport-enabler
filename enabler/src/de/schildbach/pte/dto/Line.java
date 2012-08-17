/*
 * Copyright 2010-2012 the original author or authors.
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
	private final transient char product; // TODO make true field
	public final String label;
	public final Style style;
	public final Set<Attr> attrs;
	public final String message;

	private static final String PRODUCT_ORDER = "IRSUTBPFC?";

	public Line(final String id, final String label, final Style style)
	{
		this(id, label, style, null, null);
	}

	public Line(final String id, final String label, final Style style, final String message)
	{
		this(id, label, style, null, message);
	}

	public Line(final String id, final String label, final Style style, final Set<Attr> attrs)
	{
		this(id, label, style, attrs, null);
	}

	private Line(final String id, final String label, final Style style, final Set<Attr> attrs, final String message)
	{
		this.id = id;
		this.label = label;
		this.style = style;
		this.attrs = attrs;
		this.message = message;

		product = label != null ? label.charAt(0) : '?';
	}

	public boolean hasAttr(final Attr attr)
	{
		return attrs != null && attrs.contains(attr);
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder("Line(");
		builder.append(label);
		builder.append(")");
		return builder.toString();
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Line))
			return false;
		final Line other = (Line) o;
		return nullSafeEquals(this.label, other.label);
	}

	@Override
	public int hashCode()
	{
		return nullSafeHashCode(label);
	}

	public int compareTo(final Line other)
	{
		final int productThis = PRODUCT_ORDER.indexOf(this.product);
		final int productOther = PRODUCT_ORDER.indexOf(other.product);

		final int compareProduct = new Integer(productThis >= 0 ? productThis : Integer.MAX_VALUE).compareTo(productOther >= 0 ? productOther
				: Integer.MAX_VALUE);
		if (compareProduct != 0)
			return compareProduct;

		return this.label.compareTo(other.label);
	}

	private boolean nullSafeEquals(final Object o1, final Object o2)
	{
		if (o1 == null && o2 == null)
			return true;
		if (o1 != null && o1.equals(o2))
			return true;
		return false;
	}

	private int nullSafeHashCode(final Object o)
	{
		if (o == null)
			return 0;
		return o.hashCode();
	}
}
