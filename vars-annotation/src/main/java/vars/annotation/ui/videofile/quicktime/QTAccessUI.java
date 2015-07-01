package vars.annotation.ui.videofile.quicktime;

import org.mbari.util.Tuple2;
import vars.annotation.AnnotationDAOFactory;
import vars.annotation.VideoArchive;
import vars.annotation.ui.ToolBelt;
import vars.annotation.ui.videofile.*;

import javax.inject.Inject;
import java.awt.*;
import java.util.Optional;

/**
 * Created by brian on 1/7/14.
 * @deprecated
 */
public class QTAccessUI extends AbstractAccessUI {

    private VideoPlayerDialogUI dialog;
    private Window currentParent;
    //private QTController controller = new QTController();

    @Inject
    public QTAccessUI(AnnotationDAOFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    public VideoPlayerDialogUI getOpenDialog(Window parent, ToolBelt toolBelt) {

        // dispose of old dialog if the parent window reference changes
        if (dialog != null && parent != currentParent) {
            //dialog.dispose();
            dialog = null;
        }

        // create new dialog if needed
        if (dialog == null) {
            dialog = new DefaultVideoPlayerDialogUI(parent, toolBelt, this);
            currentParent = parent;
        }

        return dialog;
    }

    @Override
    public Tuple2<VideoArchive, VideoPlayerController> openMoviePlayer(VideoParams videoParams) {
//        Optional<VideoArchive> videoArchiveOpt = findByLocation(videoParams.getMovieLocation());
//        VideoArchive videoArchive = videoArchiveOpt.orElseGet(() -> createVideoArchive(videoParams));
//        controller.getVideoControlService().connect(videoParams.getMovieLocation(),
//                videoParams.getTimeSourceOpt().orElse(TimeSource.AUTO));
//        return new Tuple2<>(videoArchive, controller);
        throw new UnsupportedOperationException("VARS is dropping direct QuickTime support");
    }
}