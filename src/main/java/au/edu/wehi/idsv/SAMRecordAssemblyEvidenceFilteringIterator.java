package au.edu.wehi.idsv;

import java.util.Iterator;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * Filters an assembly evidence stream to a subset of the records
 * @author cameron.d
 *
 */
public class SAMRecordAssemblyEvidenceFilteringIterator extends AbstractIterator<SAMRecordAssemblyEvidence> {
	private final ProcessingContext processContext;
	private final PeekingIterator<SAMRecordAssemblyEvidence> it;
	public SAMRecordAssemblyEvidenceFilteringIterator(
			ProcessingContext processContext,
			Iterator<SAMRecordAssemblyEvidence> it) {
		this.processContext = processContext;
		this.it = Iterators.peekingIterator(it); 
	}
	@Override
	protected SAMRecordAssemblyEvidence computeNext() {
		while (it.hasNext()) {
			SAMRecordAssemblyEvidence evidence = it.next();
			if (!evidence.isAssemblyFiltered() || processContext.getAssemblyParameters().writeFiltered) {
				return evidence;
			}
		}
		return endOfData();
	}
}
