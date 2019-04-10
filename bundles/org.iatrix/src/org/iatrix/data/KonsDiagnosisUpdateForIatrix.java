package org.iatrix.data;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.constants.Preferences;
import ch.elexis.core.constants.StringConstants;
import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.extension.AbstractCoreOperationAdvisor;
import ch.elexis.core.data.extension.CoreOperationExtensionPoint;
import ch.elexis.core.data.util.IRunnableWithProgress;
import ch.elexis.core.model.FallConstants;
import ch.elexis.core.model.IDiagnose;
import ch.elexis.core.model.ch.BillingLaw;
import ch.elexis.core.ui.dbcheck.external.ExternalMaintenance;
import ch.elexis.data.BillingSystem;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.icpc.Episode;
import ch.rgw.tools.StringTool;

public class KonsDiagnosisUpdateForIatrix extends ExternalMaintenance {
	
	protected static Logger log = LoggerFactory.getLogger(KonsDiagnosisUpdateForIatrix.class);
	
	/*
	 *
SELECT * FROM AT_MEDEVIT_ELEXIS_FREETEXTDIAGNOSE ;
select * from diagnosen where lastupdate is not null;
select * from ELEXISBEFUNDE where patientid = '1afb507ddd2635051d056221';
select * from  IATRIX_PROBLEM_DG_JOINT;
select * from  IATRIX_PROBLEM_BEHDL_JOINT;
select * from CH_ELEXIS_ICPC_EPISODES  where lastupdate is not null; // HIer sind die Iatrix-Diagnosen
select * from CH_ELEXIS_CORE_FINDINGS_OBSERVATION;
select * from CH_ELEXIS_ICPC;
	 */

	
	/**
	 * https://redmine.medelexis.ch/issues/14971
	 * 
	 * @since 3.8
	 */
	public KonsDiagnosisUpdateForIatrix(){}
	
	@Override
	public String executeMaintenance(IProgressMonitor pm, String DBVersion){
		StringBuilder sb = new StringBuilder();
		Query<Episode> query = new Query<Episode>(Episode.class);
		query.clear(true);
		query.add(Episode.FLD_LASTUPDATE, Query.NOT_EQUAL, null);
		query.add(Episode.FLD_DELETED, Query.EQUALS, "0"); // not deleted
		query.add(Episode.FLD_STATUS, Query.EQUALS, "1"); // status active
		List<Episode> alleIatricIcpcEpisoden = query.execute();
		log.debug("Found {} items", alleIatricIcpcEpisoden.size());
		int nrItems = alleIatricIcpcEpisoden.size();
		pm.beginTask("Moving (Iatrix) ICPC Episodes#Diagnosis ...", nrItems);
		int j = 0;
		for (Episode episode : alleIatricIcpcEpisoden) {
			j++;
			Patient actPat = episode.getPatient();
			String oldDiag = actPat.getDiagnosen();
			StringBuilder newDiag =  new StringBuilder(episode.getTitle());
			if (episode.getId().contentEquals("1")) {
				continue; // Version
			}
			if (actPat != null ) {
				List<IDiagnose> diagnoses = episode.getDiagnoses();
				if (diagnoses.size() > 0) {
					newDiag =  new StringBuilder("");
					for (IDiagnose diag : diagnoses)
					{
					    if (newDiag.toString().length() > 0 ) {
					    	newDiag.append("\n");
					    }
					    newDiag.append(diag.getLabel());
					}
				}
				if (!oldDiag.contains(newDiag)) {
					log.debug("{}/{}: id {} new {} pat {}", j, nrItems,
							episode.getId(), newDiag, actPat.getPersonalia() );
					sb.append("[" + actPat.getPersonalia() + "] Added diagnosis[" + newDiag + "]\n");
					actPat.setDiagnosen(oldDiag + StringTool.crlf + newDiag.toString());
				}
			}
			pm.worked(1);
		}
		pm.done();
		return sb.toString();
	}


	@Override
	public String getMaintenanceDescription() {
		return "[14971] Übertragung Diagnose aus Iatrix zur Patientenübersicht";
	}
	
}
