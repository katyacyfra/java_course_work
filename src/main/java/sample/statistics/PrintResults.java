package sample.statistics;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.io.*;
import java.util.List;

public class PrintResults {
    public PrintResults(String title, String key, List<Integer> xs, List<Long> values, String x, int metricNumber) {
        final XYSeries series = new XYSeries(key);
        for (int i = 0; i < xs.size(); ++i) {
            series.add(xs.get(i), values.get(i));
        }
        final XYSeriesCollection data = new XYSeriesCollection(series);
        final JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                x,
                "Time per query, ms",
                data,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        File file = new File("./results/metric" + metricNumber + ".txt");
        file.getParentFile().mkdirs();

        try (OutputStream out = new FileOutputStream("./results/metric" + metricNumber + ".png");
             PrintWriter printWriter = new PrintWriter(file)) {
            ChartUtilities.writeChartAsPNG(out, chart, 500, 300);

            //write to file
            printWriter.println(title);
            for (int i = 0; i < xs.size(); ++i) {
                printWriter.println("x = " + xs.get(i) + "  y = " + values.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}