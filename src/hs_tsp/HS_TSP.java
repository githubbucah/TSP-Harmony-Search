/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hs_tsp;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kerem Durak
 */
public class HS_TSP {

    private double PAR; // pitch adjusting range
    private double HMCR; // harmony memory considering rate
    private int maxIter;
    private int numberOfCities;
    private double[] fitnessValues;
    private Point bestFitness;
    private Point worstFitness;
    private int numberOfIterations;
    private int HMS;                      //Harmony memory size

    public TSPCoordinate[][] HarmonyMemory;
    private Vector coords;
    TSPCoordinate[] newHarmony; // plus one for the fitness. 

    private double[][] networkOfDistances;
    // private int graph[][];

    TSPFileParser parser;

    public HS_TSP(String fileName) throws TSPException {

        parser = new TSPFileParser(fileName);
        coords = parser.parseFile();
        this.HMS = 40;
        this.numberOfIterations = 5;
        this.numberOfCities = 53;
        this.fitnessValues = new double[numberOfCities];
        this.networkOfDistances = new double[numberOfCities][numberOfCities];
        this.newHarmony = new TSPCoordinate[numberOfCities + 1];
        this.bestFitness = new Point(0, 0);
        this.worstFitness = new Point(0, 0);


    }

    public void generateHM(Vector v) {

        HarmonyMemory = new TSPCoordinate[HMS][numberOfCities + 1]; // +1 for the last column that is for results.
        int j = 0;

        for (int i = 0; i < HMS; i++) { //outer, one complete

            Vector tempVector = (Vector) v.clone();   // clone vector to use it temporarily. Will temper with it.
            int tempCitySize = numberOfCities;
            TSPCoordinate routeFitness = new TSPCoordinate(0, 0, 0);
            double currentFitness = 0.0;

            for (int k = 0; k < numberOfCities; k++) { // inner, route
                int chosenOne = (int) (Math.random() * ((tempCitySize)));

                TSPCoordinate tempCoo = (TSPCoordinate) tempVector.get(chosenOne);
                HarmonyMemory[i][k] = tempCoo;
               // System.out.println("Generated city coo at k:" + k + " " + tempCoo.getX() + ", " + tempCoo.getY());

                //calculate the route fitness
                if (k != 0) {
                    currentFitness += calculateDistance(HarmonyMemory[i][k], HarmonyMemory[i][k - 1]);

                }

                //remove used city
                tempVector.remove(chosenOne);
                tempCitySize--;

            }
            routeFitness.updateFitness(currentFitness); //fitness value of one single route. (in TSPCoordinate object)
            System.out.println("Current Fitness: " + currentFitness);
            Point fitnessPoint = new Point(i, (int) currentFitness);

            updateGeneralFitness(fitnessPoint);

            HarmonyMemory[i][numberOfCities] = routeFitness;  // save the current fitness

        }
        System.out.println("Harmony Vector is created.");
    }

    public void setPAR(double PAR) {
        this.PAR = PAR;
    }

    public void setHMCR(double HMCR) {
        this.HMCR = HMCR;
    }

    public void setMaxIter(int iterations) {
        this.maxIter = iterations;
    }

    public Point memoryConsideration(int index) { // returns a coordinate that is randomly chosen from the column @index.
        int cityFromRoutes = (int) (Math.random() * ((HMS)));
        Point p = new Point(cityFromRoutes, index);
        return p;

    }

    public TSPCoordinate pitchAdjustment(int index) {
        //   System.out.println("p values: " + (int) p.getX() + ", " + (int) p.getY());

        TSPCoordinate tsp = (TSPCoordinate) newHarmony[index];
        int randomNeighbor = (int) (Math.random() * 4);

        return tsp.getFromNeighbors(randomNeighbor);

    }

    public void start() {

        int current = 1;
        Random random = new Random();
        while (current < maxIter) {
                newHarmony = new TSPCoordinate[numberOfCities+1]; //go on here

            for (int i = 0; i < numberOfCities; i++) {
                double pHMCR = random.nextDouble(); // generate local probability value for HMCR. (double between 0 and 1) 
                if (pHMCR < HMCR) {

                    //memory consideration
                    Point p = memoryConsideration(i);
                    newHarmony[i] = (TSPCoordinate) HarmonyMemory[(int) p.getX()][(int) p.getY()];
                    // System.out.println("i: " + i + " Memory considered: " + newHarmony[i].getX() + ", " + newHarmony[i].getY());

                    double pPAR = random.nextDouble();// generate local probability value for PAR. (double between 0 and 1)
                    if (pPAR < PAR) {

                        newHarmony[i] = pitchAdjustment(i);
                        //  System.out.println("i: " + i + " Pitch adjusted: " + newHarmony[i].getX() + ", " + newHarmony[i].getY());

                    }
                } else {

                 //   int randomRoute = (int) (Math.random() * (HMS));
                  //  Point tempPoint = new Point(randomRoute, i);
                    newHarmony[i] = randomSelection();
                    //   System.out.println("i: "+i);

                }

            }
            calculateFitnessForRoute();
            current++;
        }

    }
 public Vector getCoordVector() {
        return coords;
    }
   
 public void calculateFitnessForRoute() {
        double sumOfFitness = 0.0;
        for (int i = 1; i < numberOfCities; i++) {
            TSPCoordinate tempCurrent = (TSPCoordinate) newHarmony[i];
            TSPCoordinate tempPrevious = (TSPCoordinate) newHarmony[i - 1];

            sumOfFitness += calculateDistance(tempCurrent, tempPrevious);

        }
        newHarmony[numberOfCities] = new TSPCoordinate(0, 0, 0);
        newHarmony[numberOfCities].updateFitness(sumOfFitness);

        System.out.println("newly created: " + sumOfFitness);

        if (sumOfFitness < worstFitness.getY()) {

            for (int i = 0; i < numberOfCities; i++) {
                // Replace the route for the worst fitness with the newHarmony vector. But this time, new worstfitness should be recalculated. see below method call. 
                HarmonyMemory[(int) worstFitness.getX()][i] = newHarmony[i];

            }
            
            (HarmonyMemory[(int) worstFitness.getX()][numberOfCities]).updateFitness(sumOfFitness);
          //  worstFitness = new Point((int) worstFitness.getX(), (int) sumOfFitness);
            System.out.println("new fitness: "+HarmonyMemory[(int) worstFitness.getX()][numberOfCities].getFitness());
            updateFitnessHistory();
            
            printHarmonyVector();

        }

    }
    
    public void printHarmonyVector(){
         for (int i = 0; i < HMS; i++) { //outer, one complete
            
             System.out.println("FITNESS: "+ HarmonyMemory[i][numberOfCities].getFitness());
            
            
         }
    }

    public double calculateDistance(TSPCoordinate t1, TSPCoordinate t2) {

        double x1MINUSx2 = Math.pow((t1.getX() - t2.getX()), 2);
        double y1MINUSy2 = Math.pow((t1.getY() - t2.getY()), 2);
      //  System.out.println("t1 x and y: "+t1.getX() +", "+t1.getY()+" and t2 x and y: "+ t2.getX()+", "+ t2.getY());
        //System.out.println("x1-x2: "+x1MINUSx2+", and y1-y2: "+ y1MINUSy2);
        //System.out.println("Distance: "+Math.abs(Math.sqrt(x1MINUSx2 + y1MINUSy2)));

        return Math.abs(Math.sqrt(x1MINUSx2 + y1MINUSy2));

    }

    public void createCityNetwork(Vector v) {

        for (int i = 0; i < v.size(); i++) { // from ith city
            //  System.out.println("start");
            TSPCoordinate tsc1 = (TSPCoordinate) v.get(i);
            for (int j = 0; j < v.size(); j++) { //to jth city
                double localworst = 0.0;
                TSPCoordinate tsc2 = (TSPCoordinate) v.get(j);

                if (i == j) {
                    // do nothing
                } else {

                    //networkOfDistances[i][j] = calculateDistance(tsc1, tsc2);
                    tsc1.setNeighbors(tsc2);

                }

            }

        }

    }
    
    public void updateFitnessHistory(){
        TSPCoordinate[] tempArray = new TSPCoordinate[HMS];
        for (int i = 0;i <HMS;i++){
        
       tempArray[i] = HarmonyMemory[i][numberOfCities];
           // System.out.println("Worst fitness: "+worstFitness.getY()+" and current fitness: "+ tempArray[i].getFitness());
            
            if(tempArray[i].getFitness() < worstFitness.getY()){
            
           // worstFitness.getY() = tempArray[i].getFitness();
           worstFitness = new Point(i, (int)tempArray[i].getFitness());
                System.out.println("new worst fitness: "+ worstFitness.getY());
            
            }
       
        }
           
    
    
    
    
    
    }

    private void updateGeneralFitness(Point fitnessPoint) {

        if (bestFitness.getY() == 0) {
            bestFitness = fitnessPoint;
        }
        if (worstFitness.getY() == 0) {
            worstFitness = fitnessPoint;
        }

        if (fitnessPoint.getY() < bestFitness.getY()) {
            bestFitness = fitnessPoint;
        }

        if (fitnessPoint.getY() > worstFitness.getY()) {
            worstFitness = fitnessPoint;
        }

        System.out.println("BEST: " + bestFitness.getY() + " WORST: " + worstFitness.getY());

    }

    private TSPCoordinate randomSelection() {

        int randomCity = (int) (Math.random() * (numberOfCities));

        return (TSPCoordinate) coords.get(randomCity);

    }
}
