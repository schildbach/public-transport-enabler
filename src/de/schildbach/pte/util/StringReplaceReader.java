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

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * Given a string <b>pattern</b>, a string <b>replacementPattern</b> and an input stream, this class will replace all
 * occurances of <b>pattern</b> with <b>replacementPattern</b> in the inputstream. You can give multiple
 * pattern-replacementPattern pairs. Multiple pairs are done in order they are given. If first pair is "cat"-"dog" and
 * second pair is "dog"-"house", then the result will be all occurences of "cat" or "dog" will be replaced with "house".
 * 
 * @version 0.6 21 August 1997
 * @since version 0.5, Fixed error that occured when input was shorter than the pattern
 * @author Roger Whitney (<a href=mailto:whitney@cs.sdsu.edu>whitney@cs.sdsu.edu</a>)
 */

public class StringReplaceReader extends FilterReader implements Cloneable
{
	protected CharQueue outputBuffer; // holds filtered data
	protected char[] inputBuffer;
	protected int[] shiftTable; // quick search shift table
	protected int inputBufferCharCount; // number of chars in inputBuffer

	protected char[] patternToFind = null;
	protected char[] replacementPattern = null;

	protected boolean reachedEOF = false;
	protected static int EOFIndicator = -1;
	protected static int DEFAULT_BUFFER_SIZE = 1024;

	/**
	 * Create an StringReplaceReader object that will replace all occurrences ofpattern with replacementPattern in the
	 * Reader in.
	 */
	public StringReplaceReader(Reader in, String pattern, String replacementPattern)
	{
		super(in);
		patternToFind = pattern.toCharArray();
		this.replacementPattern = replacementPattern.toCharArray();

		allocateBuffers();
	}

	/**
	 * Create an StringReplaceReader object that will replace all occurrences of pattern with replacementPattern in the
	 * inputstream in.
	 */
	public StringReplaceReader(InputStream in, String pattern, String replacementPattern)
	{
		this(new BufferedReader(new InputStreamReader(in)), pattern, replacementPattern);
	}

	/**
	 * Create an StringReplaceReader object that will replace all occurrences of pattern with replacementPattern in the
	 * string input.
	 */
	public StringReplaceReader(String input, String pattern, String replacementPattern)
	{
		this(new StringReader(input), pattern, replacementPattern);
	}

	/**
	 * Returns the entire contents of the input stream.
	 */
	public String contents() throws IOException
	{
		StringBuffer contents = new StringBuffer(1024);
		int readSize = 512;

		char[] filteredChars = new char[readSize];
		int charsRead = read(filteredChars, 0, readSize);

		while (charsRead != EOFIndicator)
		{
			contents.append(filteredChars, 0, charsRead);
			charsRead = read(filteredChars, 0, readSize);
		}

		return contents.toString();
	}

	/**
	 * Adds another pattern-replacementPattern pair. All occurrences of pattern will be replaced with
	 * replacementPattern.
	 * 
	 * @exception OutOfMemoryError
	 *                if there is not enough memory to add new pattern-replacementPattern pair
	 */
	public void replace(String pattern, String replacementPattern) throws OutOfMemoryError
	{
		// Chain StringReplaceReader objects. Clone current object
		// add clone to input stream to insure it filters before this
		// object, which gets the new pattern-replacement pair
		if (patternToFind != null)
		{
			// Replace this with clone
			try
			{
				StringReplaceReader currentReplace = (StringReplaceReader) this.clone();
				in = currentReplace;
			}
			catch (CloneNotSupportedException x)
			{
			}
		}
		patternToFind = pattern.toCharArray();
		this.replacementPattern = replacementPattern.toCharArray();
		allocateBuffers();

		reachedEOF = false;
	}

	/**
	 * Read characters into a portion of an array. This method will block until some input is available, an I/O error
	 * occurs, or the end of the stream is reached.
	 * 
	 * @parm buffer Destination buffer
	 * @parm offset location in buffer to start storing characters
	 * @parm charsToRead maximum characters to read
	 * @return number of characters actually read, -1 if reah EOF on reading first character
	 * @exception IOException
	 *                if an I/O error occurs
	 */
	@Override
	public int read(char[] buffer, int offset, int charsToRead) throws IOException
	{
		int charsRead = 0;

		while ((charsRead < charsToRead) && (!eof()))
		{
			if (outputBuffer.isEmpty())
			{
				fillInputWindow();
				filterInput();
			}
			charsRead += outputBuffer.dequeue(buffer, offset + charsRead, charsToRead - charsRead);
		}
		if (charsRead > 0)
			return charsRead;
		else if (outputBuffer.size() > 0)
		{
			charsRead = outputBuffer.dequeue(buffer, offset, charsToRead);
			return charsRead;
		}
		else if ((eof()) && (inputBufferCharCount > 0) && (inputBufferCharCount < patternToFind.length))
		{
			// remaining input is less than length of pattern
			transferRemainingInputToOutputBuffer();
			System.out.println(">> End << " + outputBuffer);
			charsRead = outputBuffer.dequeue(buffer, offset, charsToRead);
			return charsRead;
		}
		else if (eof())
			return EOFIndicator;
		else
			// this should never happen
			throw new IOException("Read attempted. Did not reach EOF and " + " no chars were read");
	}

	/**
	 * Call when remaining input is less than the pattern size, so pattern can not exist in remaining input. Just shift
	 * all input to output. Assumes that have reached EOF and inputBufferCharCount < patternToFind.length
	 */
	private void transferRemainingInputToOutputBuffer()
	{
		outputBuffer.enqueue(inputBuffer, 0, inputBufferCharCount);
		inputBufferCharCount = 0;
	}

	/**
	 * Returns the next character in the inputstream with string replacement done.
	 * 
	 * @exception IOException
	 *                if error occurs reading io stream
	 */
	@Override
	public int read() throws IOException
	{
		char[] output = new char[1];
		int charsRead = read(output, 0, 1);
		if (charsRead == EOFIndicator)
			return EOFIndicator;
		else if (charsRead == 1)
			return output[0];
		else
			throw new IOException("Single Read attempted. Did not reach EOF and " + " no chars were read");

	}

	/**
	 * Determines if a previous ASCII I/O operation caught End Of File.
	 * 
	 * @return <i>true</i> if end of file was reached.
	 */
	public boolean eof()
	{
		return reachedEOF;
	}

	/**
	 * Read inpout to see if we have found the pattern. <B>Requires:</B> When this is called we have already have read
	 * first character in pattern.<BR>
	 * <B>Side Effects: </B> After attempt to find pattern, output buffer contains either the replacement pattern or all
	 * characters we konw are not part of pattern.
	 */
	protected void filterInput() throws IOException
	{
		// Use quick-search to find pattern. Fill inputBuffer with text.
		// Process all text in inputBuffer. Place processed text in
		// outputBuffer.

		int searchStart = 0;
		int windowStart = 0;
		int patternLength = patternToFind.length;

		// Search until pattern extends past end of inputBuffer
		while (searchStart < inputBufferCharCount - patternLength + 1)
		{
			boolean foundPattern = true;

			// The search
			for (int index = 0; index < patternLength; index++)
				if (patternToFind[index] != inputBuffer[index + searchStart])
				{
					foundPattern = false;
					break; // for loop
				}

			if (foundPattern)
			{
				// move text before pattern
				outputBuffer.enqueue(inputBuffer, windowStart, searchStart - windowStart);

				replacementPatternToBuffer();
				windowStart = searchStart + patternLength;
				searchStart = windowStart;
			}
			else
			{
				// look farther along in inputBuffer
				int charLocationAfterPattern = searchStart + patternLength;

				if (charLocationAfterPattern >= inputBufferCharCount)
					searchStart += 1;
				else
					searchStart += getShift(inputBuffer[charLocationAfterPattern]);
			}
		}

		if (searchStart > inputBufferCharCount)
			searchStart = inputBufferCharCount;

		// move chars already searched
		if (reachedEOF)
		{
			outputBuffer.enqueue(inputBuffer, windowStart, inputBufferCharCount - windowStart);
			inputBufferCharCount = 0;
		}
		else
		{
			outputBuffer.enqueue(inputBuffer, windowStart, searchStart - windowStart);
			System.arraycopy(inputBuffer, searchStart, inputBuffer, 0, inputBufferCharCount - searchStart);

			inputBufferCharCount = inputBufferCharCount - searchStart;
		}
	}

	/**
	 * Fill sliding input window with chars from input Read until window is full or reach EOF
	 */
	final protected void fillInputWindow() throws IOException
	{
		int charsToRead = inputBuffer.length - inputBufferCharCount;

		int firstEmptySlotInWindow = inputBufferCharCount;

		int charsRead = in.read(inputBuffer, firstEmptySlotInWindow, charsToRead);

		if (charsRead == charsToRead) // full read
		{
			inputBufferCharCount = inputBufferCharCount + charsRead;
			charsToRead = 0;
		}
		else if (charsRead > 0) // parial read
		{
			inputBufferCharCount = inputBufferCharCount + charsRead;
			charsToRead = charsToRead - charsRead;

		}
		else if (charsRead == EOFIndicator)
		{
			reachedEOF = true;
		}
		else
			throw new IOException("Read attempted. Did not reach EOF and " + " no chars were read");

	}

	/**
	 * Return the number of positions we can shift pattern when findMyShift is character in inputBuffer after the
	 * pattern
	 */
	protected int getShift(char findMyShift)
	{
		if (findMyShift >= shiftTable.length)
			return 1;
		else
			return shiftTable[findMyShift];
	}

	/**
	 * Put replacement pattern in output buffer. Subclass overrides for more complex replacement
	 */
	protected void replacementPatternToBuffer()
	{
		outputBuffer.enqueue(replacementPattern);
	}

	private void allocateBuffers()
	{
		outputBuffer = new CharQueue(DEFAULT_BUFFER_SIZE);

		inputBuffer = new char[Math.max(patternToFind.length + 1, DEFAULT_BUFFER_SIZE)];
		inputBufferCharCount = 0;
		// allocate for most ascii characters
		shiftTable = new int[126];

		// build shiftTable for quick search
		// Entry for character X contains how far to shift
		// pattern when pattern does not match text and
		// character X is the character in text after end of
		// pattern

		// Default for characters not in pattern
		for (int k = 0; k < shiftTable.length; k++)
			shiftTable[k] = patternToFind.length + 1;

		for (int k = 0; k < patternToFind.length; k++)
			if (patternToFind[k] < shiftTable.length)
				shiftTable[patternToFind[k]] = patternToFind.length - k;
	}
}
