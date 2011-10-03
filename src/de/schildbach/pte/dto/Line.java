/*
 * Copyright 2010, 2011 the original author or authors.
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
		WHEEL_CHAIR_ACCESS
	}

	private static final long serialVersionUID = -5642533805998375070L;

	final public String id;
	final private transient char product; // TODO make true field
	final public String label;
	final public int[] colors;
	final private Set<Attr> attrs;

	private static final String PRODUCT_ORDER = "IRSUTBPFC?";

	public Line(final String id, final String label, final int[] colors)
	{
		this(id, label, colors, null);
	}

	public Line(final String id, final String label, final int[] colors, final Set<Attr> attrs)
	{
		this.id = id;
		this.label = label;
		this.colors = colors;
		this.attrs = attrs;

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
		return (this.label.equals(other.label));
	}

	@Override
	public int hashCode()
	{
		return label.hashCode();
	}

	public int compareTo(final Line other)
	{
		final int productThis = PRODUCT_ORDER.indexOf(this.product);
		final int productOther = PRODUCT_ORDER.indexOf(other.product);

		final int compareProduct = new Integer(productThis >= 0 ? productThis : Integer.MAX_VALUE).compareTo(productOther >= 0 ? productOther
				: Integer.MAX_VALUE);
		if (compareProduct != 0)
			return compareProduct;

		return label.compareTo(other.label);
	}
}
