package vars.annotation.jpa;

import vars.jpa.DAO;
import vars.annotation.CameraDataDAO;
import vars.annotation.CameraData;
import org.mbari.jpaxx.EAO;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.google.inject.Inject;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 3:56:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class CameraDataDAOImpl extends DAO implements CameraDataDAO {

    @Inject
    public CameraDataDAOImpl(EAO eao) {
        super(eao);
    }

    public List<CameraData> findByFrameGrabURLContaining(String s) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("frameGrabURL", "%" + s + "%");
        return getEAO().findByNamedQuery("CameraData.findByFrameGrabURLLike", params);
    }

    public List<CameraData> findByFrameGrabURLPostfix(String postfix) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("frameGrabURL", "%" + postfix);
        return getEAO().findByNamedQuery("CameraData.findByFrameGrabURLLike", params);
    }

    public List<CameraData> findByFrameGrabURLPrefix(String prefix) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("frameGrabURL", prefix + "%");
        return getEAO().findByNamedQuery("CameraData.findByFrameGrabURLLike", params);
    }
}