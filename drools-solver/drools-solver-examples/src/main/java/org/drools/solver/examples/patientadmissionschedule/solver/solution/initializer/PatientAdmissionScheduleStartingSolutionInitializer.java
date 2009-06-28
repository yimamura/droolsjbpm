package org.drools.solver.examples.patientadmissionschedule.solver.solution.initializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.drools.WorkingMemory;
import org.drools.FactHandle;
import org.drools.solver.core.localsearch.LocalSearchSolverScope;
import org.drools.solver.core.solution.initializer.AbstractStartingSolutionInitializer;
import org.drools.solver.core.score.Score;
import org.drools.solver.core.score.DefaultHardAndSoftScore;
import org.drools.solver.examples.common.domain.PersistableIdComparator;
import org.drools.solver.examples.manners2009.domain.Manners2009;
import org.drools.solver.examples.manners2009.domain.Guest;
import org.drools.solver.examples.manners2009.domain.SeatDesignation;
import org.drools.solver.examples.manners2009.domain.Seat;
import org.drools.solver.examples.patientadmissionschedule.domain.PatientAdmissionSchedule;
import org.drools.solver.examples.patientadmissionschedule.domain.Bed;
import org.drools.solver.examples.patientadmissionschedule.domain.BedDesignation;
import org.drools.solver.examples.patientadmissionschedule.domain.AdmissionPart;

/**
 * @author Geoffrey De Smet
 */
public class PatientAdmissionScheduleStartingSolutionInitializer extends AbstractStartingSolutionInitializer {

    @Override
    public boolean isSolutionInitialized(LocalSearchSolverScope localSearchSolverScope) {
        PatientAdmissionSchedule patientAdmissionSchedule = (PatientAdmissionSchedule) localSearchSolverScope.getWorkingSolution();
        return patientAdmissionSchedule.isInitialized();
    }

    public void initializeSolution(LocalSearchSolverScope localSearchSolverScope) {
        PatientAdmissionSchedule patientAdmissionSchedule = (PatientAdmissionSchedule)
                localSearchSolverScope.getWorkingSolution();
        initializeBedDesignationList(localSearchSolverScope, patientAdmissionSchedule);
    }

    private void initializeBedDesignationList(LocalSearchSolverScope localSearchSolverScope,
            PatientAdmissionSchedule patientAdmissionSchedule) {
        WorkingMemory workingMemory = localSearchSolverScope.getWorkingMemory();
        List<BedDesignation> bedDesignationList = createBedDesignationList(patientAdmissionSchedule);
        // Assign one admissionPart at a time
        List<Bed> undesignatedBedList = patientAdmissionSchedule.getBedList();
        for (BedDesignation bedDesignation : bedDesignationList) {
            Score bestScore = DefaultHardAndSoftScore.valueOf(Integer.MIN_VALUE);
            Bed bestBed = null;

            FactHandle bedDesignationHandle = null;
            // Try every bed for that admissionPart
            // TODO by reordening the beds so index 0 has a different table then index 1 and so on,
            // this will probably be faster because perfectMatch will be true sooner
            for (Bed bed : undesignatedBedList) {
                if (bed.allowsAdmissionPart(bedDesignation.getAdmissionPart())) {
                    if (bedDesignationHandle == null) {
                        bedDesignation.setBed(bed);
                        bedDesignationHandle = workingMemory.insert(bedDesignation);
                    } else {
                        workingMemory.modifyRetract(bedDesignationHandle);
                        bedDesignation.setBed(bed);
                        workingMemory.modifyInsert(bedDesignationHandle, bedDesignation);
                    }
                    Score score = localSearchSolverScope.calculateScoreFromWorkingMemory();
                    if (score.compareTo(bestScore) > 0) {
                        bestScore = score;
                        bestBed = bed;
                    }
                }
            }
            if (bestBed == null) {
                throw new IllegalStateException("The bestBed (" + bestBed + ") cannot be null.");
            }
            workingMemory.modifyRetract(bedDesignationHandle);
            bedDesignation.setBed(bestBed);
            workingMemory.modifyInsert(bedDesignationHandle, bedDesignation);
            undesignatedBedList.remove(bestBed);
        }
        // For the GUI's combobox list mainly, not really needed
        Collections.sort(bedDesignationList, new PersistableIdComparator());
        patientAdmissionSchedule.setBedDesignationList(bedDesignationList);
    }

    private List<BedDesignation> createBedDesignationList(PatientAdmissionSchedule patientAdmissionSchedule) {
        List<BedDesignation> bedDesignationList = new ArrayList<BedDesignation>(
                patientAdmissionSchedule.getAdmissionPartList().size());
        for (AdmissionPart admissionPart : patientAdmissionSchedule.getAdmissionPartList()) {
            BedDesignation bedDesignation = new BedDesignation();
            bedDesignation.setId(admissionPart.getId());
            bedDesignation.setAdmissionPart(admissionPart);
            bedDesignationList.add(bedDesignation);
        }
        return bedDesignationList;
    }

}