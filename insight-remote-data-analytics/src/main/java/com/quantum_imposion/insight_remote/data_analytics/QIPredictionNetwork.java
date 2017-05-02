package com.quantum_imposion.insight_remote.data_analytics;


import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;


public class QIPredictionNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger(QIPredictionNetwork.class);

    public static void main(String[] args) throws Exception {

        File baseDir = new ClassPathResource(".").getFile();
        int lstmLayerSize = 20;
        int miniBatchSize = 50;

        // Load the training and testing data
        SequenceRecordReader inputReader = new CSVSequenceRecordReader(1, ",");
        inputReader.initialize(new NumberedFileInputSplit(baseDir.getAbsolutePath() + "/input_csvs/input_%d.csv", 1, 1750));
        DataSetIterator trainIter = new SequenceRecordReaderDataSetIterator(inputReader, miniBatchSize, -1, 12, true);

        SequenceRecordReader testInputReader = new CSVSequenceRecordReader(1, ",");
        testInputReader.initialize(new NumberedFileInputSplit(baseDir.getAbsolutePath() + "/input_csvs/input_%d.csv", 1751, 3500));
        DataSetIterator testIter = new SequenceRecordReaderDataSetIterator(testInputReader, miniBatchSize, -1, 12, true);

        // Create a normalizer to facilitate use of data with the network
        NormalizerMinMaxScaler normalizer = new NormalizerMinMaxScaler(0, 1);
        normalizer.fitLabel(true);
        normalizer.fit(trainIter);

        trainIter.reset();
        trainIter.setPreProcessor(normalizer);
        testIter.setPreProcessor(normalizer);

        int inputCols = trainIter.inputColumns();

        // N.B.: There may be an exception that the logger warns you about during the configuration building.
        // That is safe to ignore.

        // Create a configuration for the neural network. Parameters to play with:
        // - Layer structure, i.e. changing number of layers and types of layers
        // - Learning rate
        // - Updater type and parameters -- Currently NESTEVEROVS with momentum 0.4
        // - Size of the LSTM layers
        // - Activation functions for each layer
        // - Loss function for the output layer
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(140)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .iterations(1)
            .weightInit(WeightInit.XAVIER)
            .updater(Updater.NESTEROVS).momentum(0.4)
            .learningRate(0.00015)
            .list()
            .layer(0, new GravesLSTM.Builder().activation(Activation.HARDTANH).nIn(inputCols).nOut(inputCols)
                .build())
            .layer(1, new DenseLayer.Builder().activation(Activation.TANH).nIn(inputCols).nOut(lstmLayerSize).build())
            .layer(2, new GravesLSTM.Builder().activation(Activation.IDENTITY).nIn(lstmLayerSize).nOut(1)
                    .build())
            .layer(3, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .activation(Activation.IDENTITY).nIn(1).nOut(1).build())
            .build();

        // Create and initialize the network
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        // Tell the neural network to print score every 5 iterations
        net.setListeners(new ScoreIterationListener(5));

        // Epochs are complete loops over all of the training data
        int numEpochs = 3;

        // Train the network
        for (int i = 0; i < numEpochs; i++) {
            net.fit(trainIter);
            LOGGER.info("Epoch " + i + " complete.");
        }

        net.rnnClearPreviousState();
        DataSet testData = testIter.next();
        net.rnnTimeStep(testData.getFeatureMatrix());
        INDArray predicted = net.rnnTimeStep(testData.getFeatureMatrix());

        // Revert data back to pre-normalized values for plotting
        normalizer.revert(testData);
        normalizer.revertLabels(predicted);

        //Create plot with data
        XYSeriesCollection c = new XYSeriesCollection();
        createSeries(c, testData.getLabels(),0, "Test data");
        createSeries(c, predicted,testData.getLabels().shape()[2], "Predicted data");

        plotDataset(c);
    }

    private static XYSeriesCollection createSeries(XYSeriesCollection seriesCollection, INDArray data, int offset, String name) {
        int nRows = data.shape()[2];
        XYSeries series = new XYSeries(name);
        for (int i = 0; i < nRows; i++) {
        	// FIXME: Hack to prevent values less than 0 from showing up
            series.add(i + offset, Math.max(0, data.getDouble(i)));
        }

        seriesCollection.addSeries(series);

        return seriesCollection;
    }

    /**
     * Generate an xy plot of the datasets provided.
     */
    private static void plotDataset(XYSeriesCollection c) {

        String title = "Forcast of IO events taking longer than 32ms";
        String xAxisLabel = "Timestep";
        String yAxisLabel = "Count";
        PlotOrientation orientation = PlotOrientation.VERTICAL;
        boolean legend = true;
        boolean tooltips = false;
        boolean urls = false;
        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, c, orientation, legend, tooltips, urls);

        // Turn on autorange to make sure data fits
        chart.getXYPlot().getRangeAxis().setAutoRange(true);

        JPanel panel = new ChartPanel(chart);

        JFrame f = new JFrame();
        f.add(panel);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.pack();
        f.setTitle("Training Data");

        RefineryUtilities.centerFrameOnScreen(f);
        f.setVisible(true);
    }
}

