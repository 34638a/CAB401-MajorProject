package qut;

import jaligner.*;
import jaligner.matrix.*;
import edu.au.jacobi.pattern.*;
import jaligner.matrix.Matrix;
import n9741232.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sequential
{
    private static HashMap<String, Sigma70Consensus> consensus = new HashMap<String, Sigma70Consensus>();
    public static Series sigma70_pattern = Sigma70Definition.getSeriesAll_Unanchored(0.7);
    private static final Matrix BLOSUM_62 = BLOSUM62.Load();
    private static byte[] complement = new byte['z'];

    static
    {
        complement['C'] = 'G'; complement['c'] = 'g';
        complement['G'] = 'C'; complement['g'] = 'c';
        complement['T'] = 'A'; complement['t'] = 'a';
        complement['A'] = 'T'; complement['a'] = 't';
    }

                    
    private static List<Gene> ParseReferenceGenes(String referenceFile) throws FileNotFoundException, IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(referenceFile)));
        List<Gene> referenceGenes = new ArrayList<Gene>();
        while (true)
        {
            String name = reader.readLine();
            if (name == null)
                break;
            String sequence = reader.readLine();
            referenceGenes.add(new Gene(name, 0, 0, sequence));
            consensus.put(name, new Sigma70Consensus());
        }
        consensus.put("all", new Sigma70Consensus());
        reader.close();
        return referenceGenes;
    }

    public static boolean Homologous(PeptideSequence A, PeptideSequence B)
    {
        return SmithWatermanGotoh.align(new Sequence(A.toString()), new Sequence(B.toString()), BLOSUM_62, 10f, 0.5f).calculateScore() >= 60;
    }

	public static NucleotideSequence GetUpstreamRegion(NucleotideSequence dna, Gene gene)
    {
        int upStreamDistance = 250;
        if (gene.location < upStreamDistance)
           upStreamDistance = gene.location-1;

        if (gene.strand == 1)
            return new NucleotideSequence(java.util.Arrays.copyOfRange(dna.bytes, gene.location-upStreamDistance-1, gene.location-1));
        else
        {
            byte[] result = new byte[upStreamDistance];
            int reverseStart = dna.bytes.length - gene.location + upStreamDistance;
            for (int i=0; i<upStreamDistance; i++)
                result[i] = complement[dna.bytes[reverseStart-i]];
            return new NucleotideSequence(result);
        }
    }

	public synchronized static Match PredictPromoter(NucleotideSequence upStreamRegion)
    {
        return BioPatterns.getBestMatch(sigma70_pattern, upStreamRegion.toString());
    }

    private static void ProcessDir(List<String> list, File dir)
    {
        if (dir.exists())
            for (File file : dir.listFiles())
                if (file.isDirectory())
                    ProcessDir(list, file);
                else
                    list.add(file.getPath());
    }

    private static List<String> ListGenbankFiles(String dir)
    {
        List<String> list = new ArrayList<String>();
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
        	System.err.println("NO FILE EXISTS @ " + dir);
        	System.err.println("PATH: " + dirFile.getAbsolutePath());
        	System.exit(-1);
		}
        ProcessDir(list, dirFile);
        return list;
    }

    private static GenebankRecord Parse(String file) throws IOException
    {
        GenebankRecord record = new GenebankRecord();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        record.Parse(reader);
        reader.close();
        return record;
    }


	/**
	 * Run the program like normal from the demo set.
	 * @param referenceGenes The reference genes that have been loaded.
	 * @param fileNames Files to load.
	 * @throws IOException
	 */
	public static void runSequentially(String referenceFile, String dir) throws IOException {

		List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
		List<String> fileNames = ListGenbankFiles(dir);

		for (String filename : fileNames)
		{
			System.out.println("File: " + filename);
			GenebankRecord record = Parse(filename);

			for (Gene referenceGene : referenceGenes)
			{
				//System.out.println("Reference Name: " + referenceGene.name);
				for (Gene gene : record.genes)





					//CODE SNIPPET [Inner comparison logic]
					if (Homologous(gene.sequence, referenceGene.sequence))
					{
						NucleotideSequence upStreamRegion = GetUpstreamRegion(record.nucleotides, gene);
						Match prediction = PredictPromoter(upStreamRegion);
						if (prediction != null)
						{
							consensus.get(referenceGene.name).addMatch(prediction);
							consensus.get("all").addMatch(prediction);
						}
					}
					//CODE SNIPPET END





			}
		}
	}


	public static void runTimedParallel(String referenceFile, String dir) throws IOException {
		List<Gene> referenceGenes = ParseReferenceGenes(referenceFile);
		List<String> fileNames = ListGenbankFiles(dir);

		List<ParallelGeneResolver> resolversList = new ArrayList<>();

		for (String fileName : fileNames) {
			try {
				GenebankRecord record = Parse(fileName);

				for (Gene recordGene : record.genes) {
					for (Gene referenceGene : referenceGenes) {
						resolversList.add(new ParallelGeneResolver(referenceGene,recordGene,record,consensus));
					}
				}
			} catch (IOException e) {
				System.err.println("Unable to load record at: " + fileName);
			}
		}

		System.out.println("Complete: Resolving in parallel");
		resolversList.stream().parallel().forEach(parallelGeneResolver -> parallelGeneResolver.onRun());
		System.out.println("Complete: Results Calculated");
	}


    public static void run(String referenceFile, String dir) throws FileNotFoundException, IOException {

		//runSequentially(referenceGenes, fileNames);

		TimerLogic timerLogic = new TimerLogic();
		timerLogic.RunForIterations(100,
				() -> {
					consensus.clear();
					try {
						runSequentially(referenceFile, dir);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});

		timerLogic.printAverageTimeTakenMs();
		timerLogic.printTimeData();
		timerLogic.createLogFile("DATA_OUT_Sequential");

        for (Map.Entry<String, Sigma70Consensus> entry : consensus.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {

		long timeStart = System.currentTimeMillis();

		//testParallelClass();
		run("referenceGenes.list", "Intellij-CAB401/Ecoli");

		long timeFinish = System.currentTimeMillis();
		long timeTaken = timeFinish - timeStart;
		long timeMins = timeTaken/60000;
		long timeSecs = (timeTaken - (timeMins*60000))/1000;
		System.out.println("Complete in: " + timeTaken);
		System.out.print("Complete in: " + timeMins + ":" + (timeSecs < 10 ? '0' + timeSecs : timeSecs));
    }
}
