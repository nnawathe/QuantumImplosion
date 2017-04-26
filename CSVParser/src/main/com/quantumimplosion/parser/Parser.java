package com.quantumimplosion.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class Parser {

	private DataInserter dataInserter;

	private String dataDirectory = ".";

	public Parser(String dataDirectory)
	{
		dataInserter = new DataInserter();
		this.dataDirectory = dataDirectory;
	}

	public void parseSummary(String summaryFilePath)
	{
		File csvData = new File(dataDirectory + summaryFilePath);
		CSVParser parser;
		try {
			parser = CSVParser.parse(csvData, Charset.defaultCharset(), CSVFormat.DEFAULT.withQuote('"').withHeader());
		} catch (IOException e) {
			//Do something useful
			e.printStackTrace();
			return;
		}
		Map<String, Integer> headerMap = parser.getHeaderMap();
		for (CSVRecord record : parser)
		{
			Set<Integer> invalidColumns = Validator.validateSummaryRecord(record);
			parsePerformance(record.get(0));
//			parseData(record, "system", invalidColumns, headerMap);
		}
	}

	public void parsePerformance(String serialNum)
	{
		File csvData = new File(dataDirectory + serialNum + "-perform.csv");
		CSVParser parser;
		try {
			parser = CSVParser.parse(csvData, Charset.defaultCharset(), CSVFormat.DEFAULT.withQuote('"').withHeader());
		} catch (IOException e) {
			//Do something useful
			e.printStackTrace();
			return;
		}
		Map<String, Integer> headerMap = parser.getHeaderMap();
		for (CSVRecord record: parser)
		{
			Set<Integer> invalidColumns = Validator.validatePerformanceRecord(record);
			for (String table : CSVParserConstants.tableToHeadersMap.keySet())
			{
				if (!table.equals("system"))
					parseData(record, table, invalidColumns, headerMap);
			}
		}
	}

	private void parseData(CSVRecord record, String tableName, Set<Integer> invalidColumns, Map<String, Integer>headerMap)
	{
		List<String> headers = CSVParserConstants.tableToHeadersMap.get(tableName);
		List<Object> values = new ArrayList<Object>();
		List<Integer> argTypes = CSVParserConstants.tableToArgTypeMap.get(tableName);
		if (invalidColumns == null)
			System.out.println("Invalid Columns list is null");
		if (headerMap == null)
			System.out.println("Header Map is null");
		for (int i = 0; i < headers.size(); i++)
		{
			String header = headers.get(i);
			if (invalidColumns.contains(headerMap.get(header)))
			{
				values.add(null);
			}
			else
			{
				if (argTypes.get(i) == CSVParserConstants.STRINGTYPE)
				{
					values.add(record.get(header));
				}
				else
				{
					values.add(Double.parseDouble(record.get(header)));
				}
			}
		}
		dataInserter.insertData(tableName, values);
	}

	//Input: list of summary files
	public static void main(String[] args)
	{
		Parser p = new Parser(args[0]);
		for (int i = 1; i < args.length; i++)
		{
			p.parseSummary(args[i]);
		}

	}

}
