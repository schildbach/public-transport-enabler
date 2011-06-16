package de.schildbach.pte.util;

/* 
 * Copyright (C) 1997 Roger Whitney <whitney@cs.sdsu.edu>
 *
 * This file is part of the San Diego State University Java Library.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/**
 * This class implements a characater queue. Yes the JKD does contain a general queue. However that queue operates on
 * objects. This queue just handles char elements. Use in IO operations where converting chars to objects will be too
 * expensive.
 * 
 * @version 1.0 21 August 1997
 * @author Roger Whitney (<a href=mailto:whitney@cs.sdsu.edu>whitney@cs.sdsu.edu</a>)
 */

final public class CharQueue
{
	/*
	 * Class invariant, queueRear is the location the next queue item should be placed If the queue is not empty,
	 * queueFront is the location of the first item in the queue
	 */

	private char[] queueElements;
	private int queueFront;
	private int queueRear;
	private int elementCount; // number of elements in the queue

	public static final int DEFAULT_QUEUE_SIZE = 256;

	public CharQueue(int Size)
	{
		queueElements = new char[Size];
		queueFront = 0;
		queueRear = 0;
		elementCount = 0;
	}

	public CharQueue()
	{
		this(DEFAULT_QUEUE_SIZE);
	}

	/**
	 * Returns the current number of locations for chars in queue
	 */
	public int capacity()
	{
		return queueElements.length;
	}

	/**
	 * Returns true if the queue is empty
	 */
	public boolean isEmpty()
	{
		if (elementCount == 0)
			return true;
		else
			return false;
	}

	/**
	 * Returns true if the queue is full
	 */
	public boolean isFull()
	{
		if (elementCount >= capacity())
			return true;
		else
			return false;
	}

	/**
	 * Returns the number of chars in the queue
	 */
	public int size()
	{
		return elementCount;
	}

	/**
	 * Returns string representation of the queue
	 */
	@Override
	public String toString()
	{
		StringBuffer queueString = new StringBuffer(elementCount);
		if (queueFront < queueRear)
		{
			queueString.append(queueElements, queueFront, elementCount);
		}
		else
		{
			int elementsFromFrontToEnd = capacity() - queueFront;
			queueString.append(queueElements, queueFront, elementsFromFrontToEnd);
			queueString.append(queueElements, 0, queueRear);
		}

		return queueString.toString();
	}

	/**
	 * Returns the current number of unused locations in the queue
	 */
	public int unusedCapacity()
	{
		return capacity() - size();
	}

	/**
	 * Removes front char from the queue and returns the char
	 */
	public char dequeue()
	{
		char itemRemoved = queueElements[queueFront];
		queueFront = (queueFront + 1) % capacity();
		elementCount--;
		return itemRemoved;
	}

	/**
	 * Fills charsRemoved with chars removed from the queue. If charsRemoved is larger than queue then charsRemoved is
	 * not completely filled
	 * 
	 * @return actual number of chars put in charsRemoved
	 */
	public int dequeue(char[] charsRemoved)
	{
		return dequeue(charsRemoved, 0, charsRemoved.length);
	}

	/**
	 * Places chars from queue in charsRemoved starting at charsRemoved[offset]. Will place numCharsRequested into
	 * charsRemoved if queue has enougth chars.
	 * 
	 * @return actual number of chars put in charsRemoved
	 */
	public int dequeue(char[] charsRemoved, int offset, int numCharsRequested)
	{
		// Don't return more chars than are in the queue
		int numCharsToReturn = Math.min(numCharsRequested, elementCount);

		int numCharsAtEnd = capacity() - queueFront;

		// Are there enough characters after front pointer?
		if (numCharsAtEnd >= numCharsToReturn)
		{
			// arraycopy is about 20 times faster than coping element by element
			System.arraycopy(queueElements, queueFront, charsRemoved, offset, numCharsToReturn);
		}
		else
		{
			// Handle wrap around
			System.arraycopy(queueElements, queueFront, charsRemoved, offset, numCharsAtEnd);
			System.arraycopy(queueElements, 0, charsRemoved, offset + numCharsAtEnd, numCharsToReturn - numCharsAtEnd);
		}

		queueFront = (queueFront + numCharsToReturn) % capacity();
		elementCount = elementCount - numCharsToReturn;
		return numCharsToReturn;
	}

	/**
	 * Returns an array containing all chars in the queue. Afterwards queue is empty.
	 */
	public char[] dequeueAll()
	{
		char[] contents = new char[elementCount];
		dequeue(contents);
		return contents;
	}

	/**
	 * Returns the front char from the queue without removing it
	 */
	public char peek()
	{
		return queueElements[queueFront];
	}

	/**
	 * Adds charToAdd to the end of the queue
	 */
	public void enqueue(char charToAdd)
	{
		if (isFull())
			grow();

		queueElements[queueRear] = charToAdd;
		queueRear = (queueRear + 1) % capacity();
		elementCount++;
	}

	/**
	 * Adds charsToAdd to the end of the queue
	 */
	public void enqueue(String charsToAdd)
	{
		enqueue(charsToAdd.toCharArray());
	}

	/**
	 * Adds all elements of charsToAdd to the end of the queue
	 */
	public void enqueue(char[] charsToAdd)
	{
		enqueue(charsToAdd, 0, charsToAdd.length);
	}

	/**
	 * Adds numCharsToAdd elements of charsToAdd, starting with charsToAdd[offset] to the end of the queue
	 */
	public void enqueue(char[] charsToAdd, int offset, int numCharsToAdd)
	{
		if (numCharsToAdd > unusedCapacity())
			grow(Math.max(numCharsToAdd + 32, capacity() * 2));
		// 32 to insure some spare capacity after growing

		int numSpacesAtEnd = capacity() - queueRear;

		// Are there enough spaces after rear pointer?
		if (numSpacesAtEnd >= numCharsToAdd)
		{
			System.arraycopy(charsToAdd, offset, queueElements, queueRear, numCharsToAdd);
		}
		else
		// Handle wrap around
		{
			System.arraycopy(charsToAdd, offset, queueElements, queueRear, numSpacesAtEnd);
			System.arraycopy(charsToAdd, offset + numSpacesAtEnd, queueElements, 0, numCharsToAdd - numSpacesAtEnd);
		}

		queueRear = (queueRear + numCharsToAdd) % capacity();
		elementCount = elementCount + numCharsToAdd;
	}

	/**
	 * Clears the queue so it has no more elements in it
	 */
	public void clear()
	{
		queueFront = 0;
		queueRear = 0;
		elementCount = 0;
	}

	/**
	 * Grows the queue. Growth policy insures amortized cost per insert is O(1)
	 */
	private void grow()
	{
		// Doubling queue insures that amortized cost per insert is O(1)
		if (capacity() <= 16)
			grow(32);
		else if (capacity() <= 1024)
			grow(capacity() * 2);
		else
			grow((int) (capacity() * 1.5));
	}

	/**
	 * Grows the queue to the given new size
	 */
	private void grow(int newSize)
	{
		char[] newQueue = new char[newSize];

		if (queueFront < queueRear)
		{
			System.arraycopy(queueElements, queueFront, newQueue, 0, elementCount);
		}
		else
		{
			int elementsFromFrontToEnd = capacity() - queueFront;
			System.arraycopy(queueElements, queueFront, newQueue, 0, elementsFromFrontToEnd);
			System.arraycopy(queueElements, 0, newQueue, elementsFromFrontToEnd, queueRear);
		}

		queueElements = newQueue;
		queueFront = 0;
		queueRear = elementCount;
	}
}
