/*
 * SerializableMatrixCursor.java
 * Copyright (C) 2010-2012 Akop Karapetyan
 *
 * This file is part of Spark 360, the online gaming service client.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */

package com.akop.bach;

import java.io.Serializable;
import java.util.ArrayList;

import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;

public class SerializableMatrixCursor extends AbstractCursor implements
        Serializable
{
	private static final long serialVersionUID = 644981936699041839L;
	
	private final String[] columnNames;
	private Object[] data;
	private int rowCount = 0;
	private final int columnCount;
	
	public SerializableMatrixCursor(String[] columnNames, int initialCapacity)
	{
		this.columnNames = columnNames;
		this.columnCount = columnNames.length;
		
		if (initialCapacity < 1)
			initialCapacity = 1;
		
		this.data = new Object[columnCount * initialCapacity];
	}

	public SerializableMatrixCursor(String[] columnNames)
	{
		this(columnNames, 64);
	}
	
	private Object get(int column)
	{
		if (column < 0 || column >= columnCount)
			throw new CursorIndexOutOfBoundsException("Requested column: "
			        + column + ", # of columns: " + columnCount);
		
		if (mPos < 0)
			throw new CursorIndexOutOfBoundsException("Before first row.");
		
		if (mPos >= rowCount)
			throw new CursorIndexOutOfBoundsException("After last row.");
		
		return data[mPos * columnCount + column];
	}
	
	public RowBuilder newRow()
	{
		rowCount++;
		int endIndex = rowCount * columnCount;
		ensureCapacity(endIndex);
		int start = endIndex - columnCount;
		return new RowBuilder(start, endIndex);
	}
	
	public void addRow(Object[] columnValues)
	{
		if (columnValues.length != columnCount)
			throw new IllegalArgumentException("columnNames.length = "
			        + columnCount + ", columnValues.length = "
			        + columnValues.length);
		
		int start = rowCount++ * columnCount;
		ensureCapacity(start + columnCount);
		System.arraycopy(columnValues, 0, data, start, columnCount);
	}
	
	public void addRow(Iterable<?> columnValues)
	{
		int start = rowCount * columnCount;
		int end = start + columnCount;
		ensureCapacity(end);
		
		if (columnValues instanceof ArrayList<?>)
		{
			addRow((ArrayList<?>) columnValues, start);
			return;
		}
		
		int current = start;
		Object[] localData = data;
		for (Object columnValue : columnValues)
		{
			if (current == end)
				throw new IllegalArgumentException(
				        "columnValues.size() > columnNames.length");

			localData[current++] = columnValue;
		}
		
		if (current != end)
			throw new IllegalArgumentException(
			        "columnValues.size() < columnNames.length");
		
		// Increase row count here in case we encounter an exception.
		rowCount++;
	}
	
	private void addRow(ArrayList<?> columnValues, int start)
	{
		int size = columnValues.size();
		if (size != columnCount)
			throw new IllegalArgumentException("columnNames.length = "
			        + columnCount + ", columnValues.size() = " + size);
		
		rowCount++;
		Object[] localData = data;
		for (int i = 0; i < size; i++)
			localData[start + i] = columnValues.get(i);
	}
	
	private void ensureCapacity(int size)
	{
		if (size > data.length)
		{
			Object[] oldData = this.data;
			int newSize = data.length * 2;
			if (newSize < size)
				newSize = size;
			
			this.data = new Object[newSize];
			System.arraycopy(oldData, 0, this.data, 0, oldData.length);
		}
	}
	
	public class RowBuilder
	{
		private int index;
		private final int endIndex;
		
		RowBuilder(int index, int endIndex)
		{
			this.index = index;
			this.endIndex = endIndex;
		}
		
		public RowBuilder add(Object columnValue)
		{
			if (index == endIndex)
			{
				throw new CursorIndexOutOfBoundsException(
				        "No more columns left.");
			}
			
			data[index++] = columnValue;
			return this;
		}
	}
	
	public int getCount()
	{
		return rowCount;
	}
	
	public String[] getColumnNames()
	{
		return columnNames;
	}
	
	public String getString(int column)
	{
		return String.valueOf(get(column));
	}
	
	public short getShort(int column)
	{
		Object value = get(column);
		return (value instanceof String) ? Short.valueOf((String) value)
		        : ((Number) value).shortValue();
	}
	
	public int getInt(int column)
	{
		Object value = get(column);
		return (value instanceof String) ? Integer.valueOf((String) value)
		        : ((Number) value).intValue();
	}
	
	public long getLong(int column)
	{
		Object value = get(column);
		return (value instanceof String) ? Long.valueOf((String) value)
		        : ((Number) value).longValue();
	}
	
	public float getFloat(int column)
	{
		Object value = get(column);
		return (value instanceof String) ? Float.valueOf((String) value)
		        : ((Number) value).floatValue();
	}
	
	public double getDouble(int column)
	{
		Object value = get(column);
		return (value instanceof String) ? Double.valueOf((String) value)
		        : ((Number) value).doubleValue();
	}
	
	public boolean isNull(int column)
	{
		return get(column) == null;
	}
}