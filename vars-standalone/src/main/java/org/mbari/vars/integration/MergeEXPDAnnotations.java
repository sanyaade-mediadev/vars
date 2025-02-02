/*
 * @(#)MergeEXPDAnnotations.java   2010.06.08 at 11:08:03 PDT
 *
 * Copyright 2009 MBARI
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.mbari.vars.integration;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.inject.Injector;
import org.mbari.expd.*;
import org.mbari.expd.actions.CollateByAlternateTimecodeFunction;
import org.mbari.expd.actions.CollateByDateFunction;
import org.mbari.expd.actions.CollateByTimecodeFunction;
import org.mbari.expd.actions.CollateFunction;
import org.mbari.expd.jdbc.DAOFactoryImpl;
import org.mbari.expd.jdbc.UberDatumImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vars.DAO;
import vars.annotation.*;
import vars.annotation.ui.Lookup;
import vars.integration.MergeFunction;
import vars.integration.MergeStatus;
import vars.integration.MergeStatusDAO;
import vars.integration.MergeType;
import vars.jpa.JPAEntity;

import java.util.*;

/**
 * Implementation of the merge function
 * @author brian
 * @deprecated Use MergeEXPDAnnotations instead. It updates the EXPDMergeHistory table
 */
public class MergeEXPDAnnotations implements MergeFunction<Map<VideoFrame, UberDatum>> {

    private final double offsetSecs = 7.5;
    private Dive dive;
    private MergeStatus mergeStatus;
    private final String platform;
    private final int sequenceNumber;
    private Collection<UberDatum> uberData;
    private final boolean useHD;
    private Collection<VideoFrame> videoFrames;
    public final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Constructs ...
     *
     * @param platform
     * @param sequenceNumber
     * @param useHD
     */
    public MergeEXPDAnnotations(String platform, int sequenceNumber, boolean useHD) {
        this.platform = platform;
        this.sequenceNumber = sequenceNumber;
        this.useHD = useHD;

    }

    /**
     *
     * @param mergeType
     * @return
     */
    public Map<VideoFrame, UberDatum> apply(MergeType mergeType) {
        if (log.isDebugEnabled()) {
            log.debug("Applying " + mergeType + " merge to " + platform + " #" + sequenceNumber +
                    " [use HD = " + useHD + "]");
        }
        fetch();

        Map<VideoFrame, UberDatum> data = coallate(mergeType);

        update(data, mergeType);

        return data;
    }

    /**
     *
     * @param mergeType
     * @return
     */
    public Map<VideoFrame, UberDatum> coallate(MergeType mergeType) {
        fetch();

        Map<VideoFrame, UberDatum> data = new HashMap<VideoFrame, UberDatum>();

        switch (mergeType) {
            case CONSERVATIVE:
                data = coallateConservative();
                break;

            case OPTIMISTIC:
                data = coallateOptimistic();
                break;

            case PESSIMISTIC:
                data = coallatePessimistic();
                break;

            case PRAGMATIC:
                data = coallatePragmatic();
                break;
        }

        mergeStatus.setMergeDate(new Date());
        mergeStatus.setStatusMessage("Using " + mergeType + " merge");

        return data;
    }

    /**
     * Match by Date, then any that aren't matched, match by timecode
     */
    private Map<VideoFrame, UberDatum> coallateConservative() {

        // Merge by Date
        Map<VideoFrame, UberDatum> merged = mergeByDate(videoFrames, uberData);

        // Merge outstanding ones by timecode
        Collection<VideoFrame> leftovers = new ArrayList<VideoFrame>(videoFrames);

        leftovers.removeAll(merged.keySet());

        if (leftovers.size() > 0) {
            merged.putAll(mergeByTimecode(leftovers, uberData));
        }

        return merged;
    }

    /**
     * Match by date only
     */
    private Map<VideoFrame, UberDatum> coallateOptimistic() {
        return mergeByDate(videoFrames, uberData);
    }

    /**
     * Match by timecode only
     */
    private Map<VideoFrame, UberDatum> coallatePessimistic() {
        return mergeByTimecode(videoFrames, uberData);
    }

    /**
     * Match bogus dates by timecode, all others by date
     * @return
     */
    private Map<VideoFrame, UberDatum> coallatePragmatic() {

        // Merge annotations with bogus dates by timecode
        Collection<VideoFrame> bogusDates = Collections2.filter(videoFrames, new Predicate<VideoFrame>() {

            public boolean apply(VideoFrame input) {
                Date date = input.getRecordedDate();

                return (date == null) || date.before(dive.getStartDate()) || date.after(dive.getEndDate());
            }

        });

        log.debug(Joiner.on(", ").join(bogusDates));

        Map<VideoFrame, UberDatum> merged = mergeByTimecode(bogusDates, uberData);

        // Merge outstanding ones by timecode
        Collection<VideoFrame> leftovers = new ArrayList<VideoFrame>(videoFrames);

        leftovers.removeAll(merged.keySet());

        if (leftovers.size() > 0) {
            merged.putAll(mergeByDate(leftovers, uberData));
        }

        return merged;

    }

    private void fetch() {

        if (mergeStatus == null) {
            mergeStatus = fetchMergeStatus();
        }

        if (uberData == null) {
            uberData = fetchExpdData();
        }

        if (videoFrames == null) {
            videoFrames = fetchVarsData();
        }

    }

    private List<UberDatum> fetchExpdData() {

        // Lookup dive
        DAOFactory daoFactory = new DAOFactoryImpl();
        DiveDAO diveDAO = daoFactory.newDiveDAO();

        dive = diveDAO.findByPlatformAndDiveNumber(platform, sequenceNumber);

        // Fetch EXPD data
        UberDatumDAO uberDatumDAO = daoFactory.newUberDatumDAO();
        List<UberDatum> uberData = uberDatumDAO.fetchData(dive, useHD, offsetSecs);

        // If no cameraData is found we don't get any nav data either. In that case
        // we just fetch nav data and convert it to uberdata.
        if (uberData.size() == 0) {
            NavigationDatumDAO navigationDatumDAO = daoFactory.newNavigationDatumDAO();
            List<NavigationDatum> navigationData = navigationDatumDAO.fetchBestNavigationData(dive);
            uberData.addAll(Collections2.transform(navigationData, new Function<NavigationDatum, UberDatum>() {
                public UberDatum apply(NavigationDatum from) {
                    return new UberDatumImpl(null, from, null);
                }
            }));
        }

        return uberData;
    }

    private MergeStatus fetchMergeStatus() {
        Injector injector = (Injector) Lookup.getGuiceInjectorDispatcher().getValueObject();
        AnnotationDAOFactory annotationDAOFactory = injector.getInstance(AnnotationDAOFactory.class);
        VideoArchiveSetDAO videoArchiveSetDAO = annotationDAOFactory.newVideoArchiveSetDAO();
        DAOFactory daoFactory = new DAOFactoryImpl();
        DiveDAO diveDAO = daoFactory.newDiveDAO();
        MergeStatusDAO dao = new MergeStatusDAOImpl(annotationDAOFactory, diveDAO);
        MergeStatus myMergeStatus = dao.findByPlatformAndSequenceNumber(platform, sequenceNumber);
        dao.close();
        if (myMergeStatus == null) {
            myMergeStatus = new MergeStatus();
            videoArchiveSetDAO.startTransaction();

            Collection<VideoArchiveSet> videoArchiveSets = videoArchiveSetDAO.findAllByPlatformAndSequenceNumber(
                    platform, sequenceNumber);

            if (videoArchiveSets.size() > 0) {
                VideoArchiveSet videoArchiveSet = videoArchiveSets.iterator().next();

                myMergeStatus.setVideoArchiveSetID(((JPAEntity) videoArchiveSet).getId());
                myMergeStatus.setVideoFrameCount((long) videoArchiveSet.getVideoFrames().size());
                myMergeStatus.setStatusMessage("");
            }

            videoArchiveSetDAO.endTransaction();
        }
        videoArchiveSetDAO.close();

        return myMergeStatus;
    }

    private List<VideoFrame> fetchVarsData() {
        Injector injector = (Injector) Lookup.getGuiceInjectorDispatcher().getValueObject();
        AnnotationDAOFactory annotationDAOFactory = injector.getInstance(AnnotationDAOFactory.class);
        VideoArchiveSetDAO videoArchiveSetDAO = annotationDAOFactory.newVideoArchiveSetDAO();
        List<VideoFrame> myVideoFrames = new ArrayList<VideoFrame>();

        videoArchiveSetDAO.startTransaction();

        Collection<VideoArchiveSet> videoArchiveSets = videoArchiveSetDAO.findAllByPlatformAndSequenceNumber(platform,
                sequenceNumber);

        for (VideoArchiveSet videoArchiveSet : videoArchiveSets) {
            myVideoFrames.addAll(videoArchiveSet.getVideoFrames());
        }

        videoArchiveSetDAO.endTransaction();
        videoArchiveSetDAO.close();

        if (myVideoFrames.size() == 0) {
            mergeStatus.setStatusMessage(mergeStatus.getStatusMessage() + "; No annotations found in VARS");
        }

        return myVideoFrames;
    }

    private Map<VideoFrame, UberDatum> mergeByDate(Collection<VideoFrame> vfc, Collection<UberDatum> udc) {

        // Merge by Date
        CollateFunction<Date> f1 = new CollateByDateFunction();

        // Extract the dates from the video frames
        Collection<Date> d = Collections2.transform(vfc, new Function<VideoFrame, Date>() {
            public Date apply(VideoFrame from) {
                return from.getRecordedDate();
            }
        });

        final Map<Date, UberDatum> r1 = f1.apply(d, udc, (long) offsetSecs * 1000);

        // Associate UberDatum with VideoFrame
        final Map<VideoFrame, UberDatum> out = new LinkedHashMap<VideoFrame, UberDatum>();

        for (VideoFrame videoFrame : vfc) {
            UberDatum uberDatum = r1.get(videoFrame.getRecordedDate());
            if (uberDatum != null) {
                out.put(videoFrame, uberDatum);
            }
        }

        return out;
    }

    private Map<VideoFrame, UberDatum> mergeByTimecode(Collection<VideoFrame> vfc, Collection<UberDatum> udc) {

        // Make sure we're using the correct merge function for HD and Beta
        CollateFunction<String> f2 = useHD ? new CollateByAlternateTimecodeFunction() : new CollateByTimecodeFunction();

        Collection<String> d = Collections2.transform(vfc, new Function<VideoFrame, String>() {
            public String apply(VideoFrame from) {
                return from.getTimecode();
            }
        });

        final Map<String, UberDatum> r2 = f2.apply(d, udc, (long) offsetSecs * 1000);

        // Associate UberDatum with VideoFrame
        final Map<VideoFrame, UberDatum> out = new LinkedHashMap<VideoFrame, UberDatum>();

        for (VideoFrame videoFrame : vfc) {
            UberDatum uberDatum = r2.get(videoFrame.getTimecode());
            if (uberDatum != null) {
                out.put(videoFrame, uberDatum);
            }
        }

        return out;

    }

    /**
     *
     * @param data
     * @param mergeType
     */
    public void update(Map<VideoFrame, UberDatum> data, MergeType mergeType) {
        fetch();

        Injector injector = (Injector) Lookup.getGuiceInjectorDispatcher().getValueObject();
        AnnotationDAOFactory annotationDAOFactory = injector.getInstance(AnnotationDAOFactory.class);
        DAO dao = annotationDAOFactory.newDAO();

        dao.startTransaction();

        // Modify data
        int fixedDateCount = 0;

        for (VideoFrame videoFrame : data.keySet()) {
            UberDatum uberDatum = data.get(videoFrame);
            String nav = (uberDatum.getNavigationDatum() == null) ? "NO Navigation Data" :
                    uberDatum.getNavigationDatum().getDate() + " : Depth = " + uberDatum.getNavigationDatum().getDepth();
            String cam = (uberDatum.getCameraDatum() == null) ? "NO Camera Data" :
                    uberDatum.getCameraDatum().getTimecode() + " - " + uberDatum.getCameraDatum().getAlternativeTimecode() +
                           " - " + uberDatum.getCameraDatum().getDate();

            log.debug(videoFrame.getTimecode() + " : " + videoFrame.getRecordedDate() + " :NAV: " +
                    nav + " :CAM: " + cam);

            videoFrame = dao.find(videoFrame);

            Date recordedDate = videoFrame.getRecordedDate();

            // ---- Update cameradata
            CameraData cameraData = videoFrame.getCameraData();
            CameraDatum cameraDatum = uberDatum.getCameraDatum();

            if (cameraDatum != null) {

                cameraData.setFocus((cameraDatum.getFocus() == null) ? null : Math.round(cameraDatum.getFocus()));
                cameraData.setLogDate(cameraDatum.getDate());
                videoFrame.setAlternateTimecode(cameraDatum.getAlternativeTimecode());
                cameraData.setZoom((cameraDatum.getZoom() == null) ? null : Math.round(cameraDatum.getZoom()));
                cameraData.setIris((cameraDatum.getIris() == null) ? null : Math.round(cameraDatum.getIris()));
            }
            else {
                log.info("No camera data was found in EXPD for {}", videoFrame);
                cameraData.setFocus(null);
                cameraData.setZoom(null);
                cameraData.setIris(null);
                cameraData.setLogDate(null);
            }

            // ---- Update physicaldata
            PhysicalData physicalData = videoFrame.getPhysicalData();
            CtdDatum ctdDatum = uberDatum.getCtdDatum();

            if (ctdDatum != null) {
                physicalData.setLight(ctdDatum.getLightTransmission());
                physicalData.setOxygen(ctdDatum.getOxygen());
                physicalData.setSalinity(ctdDatum.getSalinity());
                physicalData.setTemperature(ctdDatum.getTemperature());
            }
            else {
                log.info("No CTD data was found in EXPD for {}", videoFrame);
                physicalData.setLight(null);
                physicalData.setOxygen(null);
                physicalData.setSalinity(null);
                physicalData.setTemperature(null);
            }

            NavigationDatum navigationDatum = uberDatum.getNavigationDatum();

            if (navigationDatum != null) {
                physicalData.setDepth(navigationDatum.getDepth());
                physicalData.setLatitude(navigationDatum.getLatitude());
                physicalData.setLogDate(navigationDatum.getDate());
                physicalData.setLongitude(navigationDatum.getLongitude());
            }
            else {
                log.info("No navigation data was found in EXPD for {}", videoFrame);
                physicalData.setDepth(null);
                physicalData.setLatitude(null);
                physicalData.setLogDate(null);
                physicalData.setLongitude(null);
            }

            // ---- Update date 
            switch (mergeType) {
                case PESSIMISTIC:
                    mergeStatus.setDateSource("EXPD");
                    // Change dates to ones found in EXPD
                    if (navigationDatum == null || navigationDatum.getDate() == null) {
                        videoFrame.setRecordedDate(null);
                        fixedDateCount++;
                    }
                    else if (!navigationDatum.getDate().equals(recordedDate)) {
                        videoFrame.setRecordedDate(navigationDatum.getDate());
                        fixedDateCount++;
                    }

                    break;

                case PRAGMATIC:
                    if ((recordedDate == null) || recordedDate.before(dive.getStartDate()) ||
                            recordedDate.after(dive.getEndDate())) {

                        Date date = null;
                        if (navigationDatum != null) {
                            date = navigationDatum.getDate();
                        }
                        if (date == null && cameraDatum != null) {
                            date = cameraDatum.getDate();
                        }

                        videoFrame.setRecordedDate(date);

                        fixedDateCount++;
                    }
                    break;
            }

        }

        // ---- Change unmerged dates to null
        if (MergeType.PESSIMISTIC == mergeType) {
            Collection<VideoFrame> unmerged = new ArrayList<VideoFrame>(videoFrames);

            unmerged.removeAll(data.keySet());

            for (VideoFrame videoFrame : unmerged) {
                videoFrame = dao.find(videoFrame);
                videoFrame.setRecordedDate(null);
            }
        }

        // ---- Specify the source of the data information
        switch (mergeType) {
            case PESSIMISTIC:
                mergeStatus.setDateSource("EXPD");

                break;

            default:
                mergeStatus.setDateSource("VARS");
        }

        mergeStatus.setMerged(fixedDateCount);

        if (fixedDateCount > 0) {
            mergeStatus.setDateSource("Both");
            mergeStatus.setStatusMessage(mergeStatus.getStatusMessage() + "; Fixed " + fixedDateCount +
                    " annotation dates");
        }

        dao.endTransaction();
        dao.close();

        // ---- Set the navigationedited flag
        Collection<UberDatum> rawNavRecords = Collections2.filter(data.values(), new Predicate<UberDatum>() {
            public boolean apply(UberDatum input) {
                NavigationDatum nav = input.getNavigationDatum();
                return (nav != null) && nav.isEdited() == Boolean.FALSE;
            }
        });

        mergeStatus.setNavigationEdited((rawNavRecords.size() == 0) ? 1 : 0);

        // Set merged flag
        mergeStatus.setMerged((data.size() > 0) ? 1 : 0);

        DAOFactory daoFactory = new DAOFactoryImpl();
        DiveDAO diveDAO = daoFactory.newDiveDAO();
        MergeStatusDAO mergeStatusDAO = new MergeStatusDAOImpl(annotationDAOFactory, diveDAO);

        mergeStatusDAO.update(mergeStatus);


    }

    public MergeStatus getMergeStatus() {
        fetch();
        return mergeStatus;
    }
}
