package com.quantumimplosion.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;

public class Validator {

	private static final ArrayList<Class<?>> expectedPerformanceTypes;
	
	//This is terrible, I know. Sue me
	static {
		//Hardcoded column types
		expectedPerformanceTypes = new ArrayList<Class<?>>(Arrays.asList(Double.class, String.class, String.class, String.class, String.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, String.class,
				Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class));
	}

	public static Set<Integer> validateSummaryRecord(CSVRecord record)
	{
		return null;
	}
	
	public static Set<Integer> validatePerformanceRecord(CSVRecord record)
	{
		Set<Integer> invalidColumns = new HashSet<Integer>();
		System.out.println("Record Size: " + record.size());
		for (int i = 0; i < record.size(); i++)
		{
			if (expectedPerformanceTypes.get(i) == Double.class)
			{
				try
				{
					Integer.parseInt(record.get(i));
				}
				catch (NumberFormatException nfe)
				{
					invalidColumns.add(i);
				}
			}
		}
		return invalidColumns;
	}
}