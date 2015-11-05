package parameterTuning;

public class CodeFromOriginalParamTuning {
	
	/*
	public static void tryDifferentParameters() {
		String root = System.getProperty("user.dir") + "/data/parameterTuning/";
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		Dataset trainingDataset = new Dataset(powerPlantFiles + "Folds5x2_pp.txt", true, true, 4, TRAINING_SAMPLE_FRACTION);
		//PriorityQueue<Map.Entry<Double, GbmParameters>> sortedEnsembles = new PriorityQueue<Map.Entry<Double, GbmParameters>>(new EnsembleComparator());
		int done = 0;
		for (double learningRate = 2; learningRate >= 1; learningRate-=0.5) {
			for (int numberOfSplits = 100; numberOfSplits > 0; numberOfSplits -= 10) {
				String lrbfsDirectory = root + String.format("%.5fLR/%.5fBF/%dSplits/", learningRate, BAG_FRACTION, numberOfSplits);
				new File(lrbfsDirectory).mkdirs();
				for (int i = 0; i <= 1; i++) {
					GbmParameters parameters;
					if (i == 0) {
					parameters = new GbmParameters(bagFraction, learningRate, NUMBER_OF_TREES, minExamplesInNode, numberOfSplits, LearningRatePolicy.CONSTANT);
					} else {
					parameters = new GbmParameters(BAG_FRACTION, learningRate, NUMBER_OF_TREES, MIN_EXAMPLES_IN_NODE, numberOfSplits, LearningRatePolicy.VARIABLE, SplitsPolicy.RANDOM);
					}
					timer.start();
					CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, trainingDataset, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE);
					if (ensemble != null) {
						try {
							ensemble.saveDataToFile("", lrbfsDirectory);
						} catch (IOException e) {
							e.printStackTrace();
						}
						//PlotGenerator.plotCVEnsemble(lrbfsDirectory, parameters.getFileNamePrefix(), parameters.getFileNamePrefix(), ensemble);
						System.out.println("Finished " + parameters.getFileNamePrefix() + " in " + timer.getElapsedSeconds() + " seconds");
					} else {
						System.out.println("Failed to build because of inpossible parameters " + parameters.getFileNamePrefix() + " in " + timer.getElapsedSeconds() + " seconds");
					}
					//}
					done+=2;
					System.out.println("Have been running for " + globalTimer.getElapsedMinutes() + " minutes. Completed " + done + " out of 384");
			}
		}
		GradientBoostingTree.executor.shutdownNow();
	}
	*/
	
	
	/*
	public static void readAndSortParameters() {
		try {
			Dataset trainingDataset = new Dataset(powerPlantFiles + "Folds5x2_pp.txt", true, true, 4, TRAINING_SAMPLE_FRACTION);
			StopWatch timer = new StopWatch();
			PriorityQueue<OptimalParameterRecord> sortedByCvValidationError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.CvValidationErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByCvTestError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.CvTestErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByAllDataTestError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.AllDataTestErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByAllDataTrainingError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.AllDataTrainingErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByTimeInSeconds = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.TimeInSecondsComparator());
			String root = System.getProperty("user.dir") + "/data/parameterTuning/";
			for (double learningRate = 3; learningRate >= .001; learningRate = (learningRate > 1) ? learningRate-1 : learningRate / 10) {
				for (double bagFraction = 1; bagFraction >= 0.1; bagFraction -= 0.3) {
					for (int numberOfSplits = 10; numberOfSplits > 0; numberOfSplits -= 3) {
						String lrbfsDirectory = root + String.format("%.5fLR/%.5fBF/%dSplits/", learningRate, bagFraction, numberOfSplits);
						new File(lrbfsDirectory).mkdirs();
						for (int minExamplesInNode = 1; minExamplesInNode <= 1000; minExamplesInNode *= 10) {
							for (int i = 0; i <= 1; i++) {
								OptimalParameterRecord record = new OptimalParameterRecord();
							
								GbmParameters parameters;
								if (i == 0) {
									parameters = new GbmParameters(bagFraction, learningRate, NUMBER_OF_TREES, minExamplesInNode, numberOfSplits, LearningRatePolicy.CONSTANT, SPLITS_POLICY);
								} else {
									parameters = new GbmParameters(bagFraction, learningRate, NUMBER_OF_TREES, minExamplesInNode, numberOfSplits, LearningRatePolicy.VARIABLE, SPLITS_POLICY);
								}
								try {
									// System.out.println(lrbfsDirectory + "normal/" + parameters.getFileNamePrefix() + "--normal.txt");
									BufferedReader br = new BufferedReader(new FileReader(new File(lrbfsDirectory + "normal/" + parameters.getFileNamePrefix() + "--normal.txt")));
									String mightBeTimeInSeconds = br.readLine(); // Added this later. For backwards compatability with prior data need extra checks
									double timeInSeconds = -1;
									if (mightBeTimeInSeconds.contains("TimeInSeconds")) {
										timeInSeconds = Double.parseDouble(mightBeTimeInSeconds.split(": ")[1].trim());
										br.readLine(); // Skip Step Size
									}
									br.readLine(); // Skip number of Folds
									record.totalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
									record.optimalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
									record.cvValidationError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.cvTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.allDataTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.cvTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.allDataTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.parameters = parameters;
									record.timeInSeconds = timeInSeconds;
									br.close();
								} catch (FileNotFoundException e) {
									System.out.println("File for " + parameters.getTabSeparatedPrintOut() + " not found");
									continue;
								}
								// If its an old run, extrapolate time in seconds form a short run.
								if (record.timeInSeconds < 0) {
									timer.start();
									parameters.numOfTrees = 500;
									GradientBoostingTree.crossValidate(parameters, trainingDataset, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE);
									double timeInSeconds =timer.getElapsedSeconds();
									System.out.println(parameters.getTabSeparatedPrintOut() + " took " + timeInSeconds);
									record.inferTimeInSecondsFromPartialRun(500, timeInSeconds);
									parameters.numOfTrees = NUMBER_OF_TREES; // Set back so its correct int he file.
								}
								sortedByCvValidationError.add(record);
								sortedByAllDataTestError.add(record);
								sortedByAllDataTrainingError.add(record);
								sortedByTimeInSeconds.add(record);
								sortedByCvTestError.add(record);
							}
						}
					}
				}
			}
			recordSortedParameters("SortedByCvValidationError", sortedByCvValidationError);
			recordSortedParameters("SortedByCvTestError", sortedByCvTestError);
			recordSortedParameters("SortedByAllDataTestError", sortedByAllDataTestError);
			recordSortedParameters("SortedByAllDataTrainingError", sortedByAllDataTrainingError);
			recordSortedParameters("SortedByTimeInSeconds", sortedByTimeInSeconds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	*/
}
