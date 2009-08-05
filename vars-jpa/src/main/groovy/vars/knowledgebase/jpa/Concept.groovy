package vars.knowledgebase.jpa

import javax.persistence.Version
import javax.persistence.Column
import java.sql.Timestamp
import javax.persistence.Id
import javax.persistence.GeneratedValue
import javax.persistence.TableGenerator
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToMany
import javax.persistence.ManyToOne
import javax.persistence.JoinColumn
import javax.persistence.GenerationType
import javax.persistence.FetchType
import javax.persistence.CascadeType
import javax.persistence.OneToOne
import vars.knowledgebase.IConceptName
import vars.knowledgebase.IConcept

/**
 *
 * <pre>
 * CREATE TABLE CONCEPT (
 *   ID                     	BIGINT NOT NULL,
 *   PRIMARYCONCEPTNAMEID_FK	BIGINT,
 *   PARENTCONCEPTID_FK     	BIGINT,
 *   ORIGINATOR             	VARCHAR(255),
 *   STRUCTURETYPE          	VARCHAR(10),
 *   REFERENCE              	VARCHAR(1024),
 *   NODCCODE               	VARCHAR(20),
 *   RANKNAME               	VARCHAR(20),
 *   RANKLEVEL              	VARCHAR(20),
 *   TAXONOMYTYPE           	VARCHAR(20),
 *   CONCEPTDELEGATEID_FK   	BIGINT,
 *   CONSTRAINT CONCEPT_PK PRIMARY KEY(ID)
 * )
 * GO
 * CREATE INDEX IDX_PRIMARYCONCEPTNAME
 *   ON CONCEPT(PRIMARYCONCEPTNAMEID_FK)
 * GO
 * CREATE INDEX IDX_PARENT_CONCEPTID
 *   ON CONCEPT(PARENTCONCEPTID_FK)
 * GO
 * CREATE INDEX IDX_CONCEPTDELEGATE
 *   ON CONCEPT(CONCEPTDELEGATEID_FK)
 * GO
 * </pre>
 */
@Entity(name = "Concept")
@Table(name = "Concept")
@NamedQueries( value = [
    @NamedQuery(name = "Concept.findById",
                query = "SELECT v FROM Concept v WHERE v.id = :id"),
    @NamedQuery(name = "Concept.findByOriginator", query = "SELECT c FROM Concept c WHERE c.originator = :originator") ,
    @NamedQuery(name = "Concept.findByStructureType",
                query = "SELECT c FROM Concept c WHERE c.structureType = :structureType") ,
    @NamedQuery(name = "Concept.findByReference", query = "SELECT c FROM Concept c WHERE c.reference = :reference") ,
    @NamedQuery(name = "Concept.findByNodcCode", query = "SELECT c FROM Concept c WHERE c.nodcCode = :nodcCode") ,
    @NamedQuery(name = "Concept.findByRankName", query = "SELECT c FROM Concept c WHERE c.rankName = :rankName") ,
    @NamedQuery(name = "Concept.findByRankLevel", query = "SELECT c FROM Concept c WHERE c.rankLevel = :rankLevel") ,
    @NamedQuery(name = "Concept.findByTaxonomyType",
                query = "SELECT c FROM Concept c WHERE c.taxonomyType = :taxonomyType")
])
class Concept implements Serializable {

    @Id
    @Column(name = "id", nullable = false, updatable=false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "Concept_Gen")
    @TableGenerator(name = "Concept_Gen", table = "UniqueID",
            pkColumnName = "TableName", valueColumnName = "NextID",
            pkColumnValue = "Concept", allocationSize = 1)
    Long id

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime

    @ManyToOne(optional = true)
    @JoinColumn(name = "ParentConceptID_FK")
    Concept parentConcept

    @OneToMany(targetEntity = Concept.class,
            mappedBy = "parentConcept",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    Set<IConcept> childConcepts

    @OneToMany(targetEntity = ConceptName.class,
            mappedBy = "concept",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL)
    Set<ConceptName> conceptNames

    @Column(name = "Field", length = 255)
    String field

    @Column(name = "Originator", length = 255)
    String originator

    @Column(name = "StructureType", length = 10)
    String structureType

    @Column(name = "Reference", length = 1024)
    String reference

    @Column(name = "NodcCode", length = 20)
    String nodcCode

    @Column(name = "RankName", length = 20)
    String rankName

    @Column(name = "RankLevel", length = 20)
    String rankLevel

    @Column(name = "TaxonomyType", length = 20)
    String taxonomyType

    @OneToOne(mappedBy = "concept", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    ConceptDelegate conceptDelegate

    ConceptDelegate getConceptDelegate() {
        if (conceptDelegate == null) {
            conceptDelegate = new ConceptDelegate()
        }
        return conceptDelegate
    }

    Set<IConceptName> getConceptNames() {
        if (conceptNames == null) {
            conceptNames = new HashSet<IConceptName>()
        }
        return conceptNames
    }

    void addConceptName(ConceptName conceptName) {
        if (getConceptNames().find { IConceptName cn -> cn.name.equals(conceptName.name) }) {
            throw new IllegalArgumentException("A ConceptName with the name '${conceptName.name}' already exists in ${this}")
        }
        getConceptNames() << conceptName
        conceptName.concept = this
    }

    void removeConceptName(ConceptName conceptName) {
        if (getConceptNames().remove(conceptName)) {
            conceptName.concept = null
        }
    }

}