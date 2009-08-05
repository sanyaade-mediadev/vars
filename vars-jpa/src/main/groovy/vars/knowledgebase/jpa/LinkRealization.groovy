package vars.knowledgebase.jpa

import javax.persistence.Id
import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.TableGenerator
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.GenerationType
import javax.persistence.Version
import java.sql.Timestamp
import javax.persistence.ManyToOne
import javax.persistence.JoinColumn
import vars.ILink
import vars.ILink;

/**
 * <pre>
 * CREATE TABLE LINKREALIZATION (
 *   ID                        	BIGINT NOT NULL,
 *   CONCEPTDELEGATEID_FK      	BIGINT,
 *   PARENTLINKREALIZATIONID_FK	BIGINT,
 *   LINKNAME                  	VARCHAR(50),
 *   TOCONCEPT                 	VARCHAR(50),
 *   LINKVALUE                 	VARCHAR(255),
 *   CONSTRAINT PK_LINKREALIZATION PRIMARY KEY(ID)
 * )
 * GO
 * CREATE INDEX IDX_CONCEPTDELEGATE3
 *   ON LINKREALIZATION(CONCEPTDELEGATEID_FK)
 * GO
 * </pre>
 */
@Entity(name = "LinkRealization")
@Table(name = "LinkRealization")
@NamedQueries( value = [
    @NamedQuery(name = "LinkRealization.findById",
                query = "SELECT v FROM LinkRealization v WHERE v.id = :id"),
    @NamedQuery(name = "LinkRealization.findByLinkName",
                query = "SELECT l FROM LinkRealization l WHERE l.linkName = :linkName") ,
    @NamedQuery(name = "LinkRealization.findByToConcept",
                query = "SELECT l FROM LinkRealization l WHERE l.toConcept = :toConcept") ,
    @NamedQuery(name = "LinkRealization.findByLinkValue",
                query = "SELECT l FROM LinkRealization l WHERE l.linkValue = :linkValue")
])
class LinkRealization implements Serializable, ILink {

    @Id
    @Column(name = "id", nullable = false, updatable=false)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "LinkTemplate_Gen")
    @TableGenerator(name = "LinkTemplate_Gen", table = "UniqueID",
            pkColumnName = "TableName", valueColumnName = "NextID",
            pkColumnValue = "LinkTemplate", allocationSize = 1)
    Long id

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime

    @Column(name = "LinkName", length = 50)
    String linkName

    @Column(name = "ToConcept", length = 50)
    String toConcept

    @Column(name = "LinkValue", length = 255)
    String linkValue

    @ManyToOne(optional = false)
    @JoinColumn(name = "ConceptDelegateID_FK")
    ConceptDelegate conceptDelegate

    public String getFromConcept() {
        return conceptDelegate?.concept
    }
}
