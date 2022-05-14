import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

public class FloydWarshall {
    private static final int I = Integer.MAX_VALUE; // Infinity
    private static final int numThreads = 64;
    private static final int dim = 5000;
    private static double fill = 0.3;
    private static int maxDistance = 100;
    private static int adjacencyMatrix[][] = new int[dim][dim];
    private static int d[][] = new int[dim][dim];
    private static PrintStream so;
    private static PrintStream mo;

    /**
     * Generate a randomized matrix to use for the algorithm.
     */
    private static void generateMatrix() {
        Random random = new Random(1);
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (i != j)
                    adjacencyMatrix[i][j] = I;
            }
        }
        for (int i = 0; i < dim * dim * fill; i++) {
            adjacencyMatrix[random.nextInt(dim)][random.nextInt(dim)] = random.nextInt(maxDistance + 1);
        }
    }

    /**
     * Execution of Floyd-Warshall sequentially
     *
     */
    private static void execute() {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                d[i][j] = adjacencyMatrix[i][j];
                if (i == j) {
                    d[i][j] = 0;
                }
            }
        }
        for (int k = 0; k < dim; k++) {
            for (int i = 0; i < dim; i++) {
                if(d[i][k] != I) {
                    for (int j = 0; j < dim; j++) {
                        if (d[k][j] == I) {
                            continue;
                        } else if (d[i][j] > d[i][k] + d[k][j]) {
                            //so.printf("updates %d,%d from %d to %d   parents %d, %d  and %d %d \n",i, j, d[i][j],  d[i][k] + d[k][j], i, k, k, j);
                            d[i][j] = d[i][k] + d[k][j];

                        }
                    }
                }
            }
        }
    }

    /**
     * Print helper method
     *
     * @param matrix
     */
    private static void print(int matrix[][]) {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (matrix[i][j] == I) {
                    System.out.print("   I ");
                } else {
                    System.out.printf("%4d ", matrix[i][j]);
                }
            }
            System.out.println();
        }
    }

    /**
     * compare resulting matix from single threaded operation and multi threaded operation
     *
     * @param matrix1
     * @param matrix2
     */
    private static void compare(int matrix1[][], int matrix2[][]) {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (matrix1[i][j] != matrix2[i][j]) {
                    System.out.printf("Comparison failed at %d, %d\n", i, j);
                    System.out.printf("Expected %d %d\n", matrix1[i][j] , matrix2[i][j]);
                    return;
                }
            }
        }
        System.out.println("Comparison succeeded");
    }


    /**
     * Driver
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        mo = new PrintStream(new File("multi.txt"));//for testing
        so = new PrintStream(new File("single.txt"));//for testing
        long start, end;
        generateMatrix();
        start = System.nanoTime();
        execute();
        int [][] single = new int[d.length][d.length];
        end = System.nanoTime();
        System.out.println("time consumed: " + (double) (end - start) / 1000000000);

        deepCopy(single, d);
        start = System.nanoTime();
        executeParallel();
        end = System.nanoTime();
        System.out.println("time consumed: " + (double) (end - start) / 1000000000);

        compare(single, d);

        so.close();
        mo.close();
    }


    /**
     * Copying 2D array manually
     *
     * @param dest  destination for copy
     * @param src  source to be copied
     */
    private static void deepCopy(int[][] dest, int[][] src) {
        for(int i = 0 ; i < dest.length ; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, src[i].length);
        }
    }


    /**
     * Parallel execution of Floyd-Warshall algo
     */
    private static void executeParallel() {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                d[i][j] = adjacencyMatrix[i][j];
                if (i == j) {
                    d[i][j] = 0;
                }
            }
        }

        FWT[] thread = new FWT[numThreads];
        //problem set evenly divided by number of threads
        int slice = dim / numThreads;

        //partitioning slice and setting work space for thread
        for(int i = 0 ; i < numThreads ; i++) {
            int start = i * slice;
            int end = (i + 1) * slice;
            if(i == numThreads - 1) {
                end = dim;
            }
            thread[i] = new FWT(start, end, 1);
            thread[i].start();
        }


        try {
            for(int i = 0 ; i < numThreads ; i++) {
                thread[i].join();
            }
            System.out.println("Joined all the threads");

            for(int i = 0 ; i < numThreads ; i++) {
                int start = i * slice;
                int end = (i + 1) * slice;
                if(i == numThreads -1) {
                    end = dim;
                }
                thread[i] = new FWT(start, end, 1);
                thread[i].start();
            }
            for(int i = 0 ; i < numThreads ; i++) {
                thread[i].join();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Floyd-Warshall algo execution by individual thread
     */
    static class FWT extends Thread {

        int start;
        int end;
        int thread=0;
        /**
         * @param start section to be executed by each thread
         * @param end
         */
        public FWT(int start, int end, int n) {
            super();
            this.start = start;
            this.end = end;
            thread = n;

        }

        @Override
        public void run() {
            for (int k = 0; k < dim; k++) {
                for (int i = start; i < end; i++) {
                    if (d[i][k] != I) {
                        for (int j = 0; j < dim; j++) {
                            if (d[k][j] == I) {
                                continue;
                            } else if (d[i][j] > d[i][k] + d[k][j]) {
                                d[i][j] = d[i][k] + d[k][j];
                            }
                        }
                    }
                }
            }
        }
    }
}