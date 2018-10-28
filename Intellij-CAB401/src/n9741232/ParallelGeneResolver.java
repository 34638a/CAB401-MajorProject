package n9741232;

import edu.au.jacobi.pattern.Match;
import qut.*;

import java.util.HashMap;

import static qut.Sequential.PredictPromoter;

/**
 * Created by Jordan Laptop on 18/10/2018.
 */
public class ParallelGeneResolver {

	private Gene referenceGene;
	private Gene recordGene;
	private GenebankRecord record;

	private HashMap<String, Sigma70Consensus> consensus;

	public ParallelGeneResolver(Gene referenceGene, Gene recordGene, GenebankRecord record, HashMap<String, Sigma70Consensus> consensus) {
		this.referenceGene = referenceGene;
		this.recordGene = recordGene;
		this.record = record;
		this.consensus = consensus;
	}

	public void onRun() {

		if (Sequential.Homologous(recordGene.sequence, referenceGene.sequence)) {
			NucleotideSequence upStreamRegion = Sequential.GetUpstreamRegion(record.nucleotides, recordGene);
			Match prediction = PredictPromoter(upStreamRegion);
			if (prediction != null) {
				consensus.get(referenceGene.name).addMatch(prediction);
				consensus.get("all").addMatch(prediction);
			}
		}
	}
}
