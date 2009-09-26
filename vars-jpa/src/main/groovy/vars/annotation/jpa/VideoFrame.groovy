package vars.annotation.jpa

import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Id
import javax.persistence.GeneratedValue
import javax.persistence.Column
import javax.persistence.OneToOne
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.GenerationType
import javax.persistence.Temporal
import javax.persistence.TemporalType
import javax.persistence.JoinColumn
import javax.persistence.FetchType
import javax.persistence.CascadeType
import javax.persistence.Version
import java.sql.Timestamp
import javax.persistence.TableGenerator
import vars.annotation.IVideoFrame
import vars.annotation.IObservation
import vars.annotation.ICameraData
import vars.annotation.IPhysicalData
import vars.annotation.IVideoArchive
import vars.jpa.JPAEntity
import vars.EntitySupportCategory
import javax.persistence.EntityListeners
import org.mbari.jpaxx.TransactionLogger
import vars.jpa.KeyNullifier
import vars.jpa.KeyNullifier
import javax.persistence.Transient

@Entity(name = "VideoFrame")
@Table(name = "VideoFrame")
@EntityListeners( value = [TransactionLogger.class, KeyNullifier.class] )
@NamedQueries( value = [
    @NamedQuery(name = "VideoFrame.findById",
                query = "SELECT v FROM VideoFrame v WHERE v.id = :id"),
    @NamedQuery(name = "VideoFrame.findByName",
                query = "SELECT v FROM VideoFrame v WHERE v.recordedDate = :recordedDate"),
    @NamedQuery(name = "VideoFrame.findByVideoArchivePrimaryKey",
                query = "SELECT v FROM VideoFrame v WHERE v.videoArchive.id = :primaryKey")
])
class VideoFrame implements Serializable, IVideoFrame, JPAEntity {

    @Transient
    private static final PROPS = Collections.unmodifiableList([IVideoFrame.PROP_TIMECODE,
            IVideoFrame.PROP_RECORDED_DATE])    


    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "VideoFrame_Gen")
    @TableGenerator(name = "VideoFrame_Gen", table = "UniqueID",
            pkColumnName = "TableName", valueColumnName = "NextID",
            pkColumnValue = "VideoFrame", allocationSize = 1)
    Long id

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime

    @Column(name = "RecordedDTG")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date recordedDate

    @Column(name = "TapeTimeCode", nullable = false, length = 11)
    String timecode

    @Column(name = "HDTimeCode", nullable = false, length = 11)
    String alternateTimecode

    @Column(name = "inSequence")
    Short inSequence

    @OneToOne(mappedBy = "videoFrame", fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = CameraData.class)
    private ICameraData cameraData

    @OneToOne(mappedBy = "videoFrame", fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = PhysicalData.class)
    private IPhysicalData physicalData

    @ManyToOne(optional = false, targetEntity = VideoArchive.class)
    @JoinColumn(name = "VideoArchiveID_FK")
    IVideoArchive videoArchive

    @OneToMany(targetEntity = Observation.class,
            mappedBy = "videoFrame",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL)
    Set<IObservation> observations

    ICameraData getCameraData() {
        if (cameraData == null) {
            cameraData = new CameraData()
        }
        return cameraData
    }

    IPhysicalData getPhysicalData() {
        if (physicalData == null) {
            physicalData = new PhysicalData()
        }
        return physicalData
    }

    Set<IObservation> getObservations() {
        if (observations == null) {
            observations = new HashSet<IObservation>()
        }
        return observations
    }

    void addObservation(IObservation observation) {
        getObservations() << observation
        observation.videoFrame = this
    }

    void removeObservation(IObservation observation) {
        if (observations?.remove(observation)) {
            observation.videoFrame = null
        }
    }

    boolean hasFrameGrab() {
        return cameraData?.frameGrabURL != null
    }

    boolean isInSequence() {
        return inSequence != 0
    }

    void setInSequence(boolean state) {
        this.inSequence = state ? 1 : 0
    }

    @Override
    String toString() {
        return EntitySupportCategory.basicToString(this, PROPS)
    }

    @Override
    boolean equals(that) {
        return EntitySupportCategory.equals(this, that, PROPS)
    }

    @Override
    int hashCode() {
        return EntitySupportCategory.hashCode(this, PROPS)
    }

}