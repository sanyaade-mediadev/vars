/*
 * @(#)ConceptMetadataImpl.java   2009.11.10 at 10:06:20 PST
 *
 * Copyright 2009 MBARI
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package vars.knowledgebase.jpa;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.hibernate.collection.PersistentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vars.EntitySupportCategory;
import vars.jpa.JPAEntity;
import vars.jpa.KeyNullifier;
import vars.jpa.TransactionLogger;
import vars.knowledgebase.Concept;
import vars.knowledgebase.ConceptMetadata;
import vars.knowledgebase.History;
import vars.knowledgebase.HistoryCreationDateComparator;
import vars.knowledgebase.LinkRealization;
import vars.knowledgebase.LinkTemplate;
import vars.knowledgebase.Media;
import vars.knowledgebase.MediaTypes;
import vars.knowledgebase.Usage;

/**
 * <pre>
 * CREATE TABLE CONCEPTDELEGATE (
 *   ID                 BIGINT NOT NULL,
 *   CONCEPTID_FK       BIGINT,
 *   USAGEID_FK         BIGINT,
 *   CONSTRAINT PK_CONCEPTDELEGATE PRIMARY KEY(ID)
 * )
 * GO
 * CREATE INDEX IDX_USAGEID
 *   ON CONCEPTDELEGATE(USAGEID_FK)
 * GO
 * CREATE INDEX IDX_CONCEPTID
 *   ON CONCEPTDELEGATE(CONCEPTID_FK)
 * GO
 * </pre>
 */
@Entity(name = "ConceptMetadata")
@Table(name = "ConceptDelegate")
@EntityListeners({ TransactionLogger.class, KeyNullifier.class })
@NamedQueries({ @NamedQuery(name = "ConceptMetadata.findById",
                            query = "SELECT v FROM ConceptMetadata v WHERE v.id = :id") })
public class ConceptMetadataImpl implements Serializable, ConceptMetadata, JPAEntity {

    @Transient
    private final Logger log = LoggerFactory.getLogger(getClass());

    @OneToOne(optional = false, targetEntity = ConceptImpl.class)
    @JoinColumn(name = "ConceptID_FK", nullable = false)
    private Concept concept;

    @OneToMany(
        targetEntity = GHistory.class,
        mappedBy = "conceptMetadata",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL
    )
    @OrderBy(value = "creationDate")
    private Set<History> histories;

    @Id
    @Column(
        name = "id",
        nullable = false,
        updatable = false
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "ConceptDelegate_Gen")
    @TableGenerator(
        name = "ConceptDelegate_Gen",
        table = "UniqueID",
        pkColumnName = "TableName",
        valueColumnName = "NextID",
        pkColumnValue = "ConceptDelegate",
        allocationSize = 1
    )
    private Long id;

    @OneToMany(
        targetEntity = GLinkRealization.class,
        mappedBy = "conceptMetadata",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL
    )
    private Set<LinkRealization> linkRealizations;

    @OneToMany(
        targetEntity = LinkTemplateImpl.class,
        mappedBy = "conceptMetadata",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL
    )
    private Set<LinkTemplate> linkTemplates;


    @OneToMany(
        targetEntity = GMedia.class,
        mappedBy = "conceptMetadata",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL
    )
    private Set<Media> medias;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime;
    @OneToOne(
        mappedBy = "conceptMetadata",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        targetEntity = GUsage.class
    )
    private Usage usage;

    public void addHistory(History history) {
        if (getHistories().add(history)) {
            ((GHistory) history).setConceptMetadata(this);
        }
    }

    public void addLinkRealization(LinkRealization linkRealization) {
        if (getLinkRealizations().add(linkRealization)) {
            ((GLinkRealization) linkRealization).setConceptMetadata(this);
        }
    }

    public void addLinkTemplate(LinkTemplate linkTemplate) {
        if (getLinkTemplates().add(linkTemplate)) {
            ((LinkTemplateImpl) linkTemplate).setConceptMetadata(this);
        }
    }

    public void addMedia(Media media) {
        if (getMedias().add(media)) {
            ((GMedia) media).setConceptMetadata(this);
        }
    }

    @Override
    public boolean equals(Object that) {

        boolean isEqual = true;

        if (this == that) {

            // Do nothing isEqual is already true
            //isEqual = true
        }
        else if ((that == null) || (this.getClass() != that.getClass())) {
            isEqual = false;
        }
        else {

            /*
             * Check ID. If they are both null use concept id
             */
            JPAEntity thatCm = (JPAEntity) that;
            if ((this.id != null) && (thatCm.getId() != null)) {
                isEqual = this.id.equals(thatCm.getId());
            }
            else {
                Concept thisConcept = getConcept();
                Concept thatConcept = ((ConceptMetadata) that).getConcept();
                if ((thisConcept == null) || (thatConcept == null)) {
                    isEqual = false;
                }
                else {
                    isEqual = thisConcept.hashCode() == thatConcept.hashCode();
                }
            }
        }

        return isEqual;

    }

    public Concept getConcept() {
        return concept;
    }

    public Set<History> getHistories() {
        if (histories == null) {
            histories = new TreeSet<History>(new HistoryCreationDateComparator());
        }
        else {
            histories = rebuildPersistentSet(histories);
        }

        return histories;
    }

    public Long getId() {
        return id;
    }

    public Set<LinkRealization> getLinkRealizations() {
        if (linkRealizations == null) {
            linkRealizations = new HashSet<LinkRealization>();
        }
        else {
            linkRealizations = rebuildPersistentSet(linkRealizations);
        }

        return linkRealizations;
    }

    public Set<LinkTemplate> getLinkTemplates() {
        if (linkTemplates == null) {
            linkTemplates = new HashSet<LinkTemplate>();
        }
        else {
            linkTemplates = rebuildPersistentSet(linkTemplates);
        }

        return linkTemplates;
    }

    public Set<Media> getMedias() {
        if (medias == null) {
            medias = new HashSet<Media>();
        }
        else {
            medias = rebuildPersistentSet(medias);
        }

        return medias;
    }

    public Media getPrimaryImage() {
        Media media = null;
        Collection<Media> m = new ArrayList<Media>(getMedias());
        for (Media media1 : m) {
            if (media1.isPrimary() && media1.getType().equalsIgnoreCase(MediaTypes.IMAGE.toString())) {
                media = media1;

                break;
            }
        }

        return media;
    }

    public Media getPrimaryMedia(MediaTypes mediaType) {
        Media primaryMedia = null;
        Set<Media> ms = new HashSet<Media>(getMedias());
        for (Media media : ms) {
            if (media.isPrimary() && media.getType().equals(mediaType.toString())) {
                primaryMedia = media;
            }
        }

        return primaryMedia;
    }

    public Usage getUsage() {
        return usage;
    }

    public boolean hasPrimaryImage() {
        return (getPrimaryImage() != null);
    }

    @Override
    public int hashCode() {
        int result = 0;

        /*
         * Use id has hash. If it's null use the concept id hash instead
         */
        if (id != null) {
            result = 3 * id.intValue();
        }
        else {
            result = (concept == null) ? 0 : concept.hashCode();
        }

        return result;

    }

    public boolean isPendingApproval() {
        boolean isPending = false;
        for (History history : getHistories()) {
            if (!history.isApproved() && !history.isRejected()) {
                isPending = true;

                break;
            }
        }

        return isPending;
    }

    /**
     * Workaround for Hibernate bug: http://opensource.atlassian.com/projects/hibernate/browse/HHH-3799
     * @param urls
     */
    private <T> Set<T> rebuildPersistentSet(Set<T> urls) {
        if (!(urls instanceof HashSet)) {
            for (Object object : urls) {
                object.hashCode();
            }
            urls = new HashSet<T>(urls);
            log.debug("Rebuilding persistentset");

        }
        return urls;
    }

    public void removeHistory(History history) {
        if (getHistories().remove(history)) {
            ((GHistory) history).setConceptMetadata(null);
        }
    }

    public void removeLinkRealization(LinkRealization linkRealization) {
        if (getLinkRealizations().remove(linkRealization)) {
            ((GLinkRealization) linkRealization).setConceptMetadata(null);
        }
    }

    public void removeLinkTemplate(LinkTemplate linkTemplate) {
        if (getLinkTemplates().remove(linkTemplate)) {
            ((LinkTemplateImpl) linkTemplate).setConceptMetadata(null);
        }
    }

    public void removeMedia(Media media) {
        if (getMedias().remove(media)) {
            ((GMedia) media).setConceptMetadata(null);
        }
    }

    void setConcept(Concept concept) {
        this.concept = concept;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsage(Usage usage) {
        if (this.usage != null) {
            GUsage thisUsage = (GUsage) this.usage;
            thisUsage.setConceptMetadata(null);
        }

        this.usage = usage;

        if (usage != null) {
            ((GUsage) usage).setConceptMetadata(this);
        }
    }

    @Override
    public String toString() {
        return EntitySupportCategory.basicToString(this, new ArrayList());
    }
}
