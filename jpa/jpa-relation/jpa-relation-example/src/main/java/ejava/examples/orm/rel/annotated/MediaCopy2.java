package ejava.examples.orm.rel.annotated;

import javax.persistence.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ejava.examples.orm.rel.MediaCopyPK2;

/**
 * This version of MediaCopy uses the @MapsId annotation to link the 
 * relationship and primary key properties together. This technique 
 * was added in JPA 2.0 and is stated to be preferred over the JPA 1.0
 * indirect technique.
 */

@Entity @Table(name="ORMREL_MEDIACOPY2")
@IdClass(MediaCopyPK2.class)
@AttributeOverrides({
	@AttributeOverride(name="copyNo", column=@Column(name="COPY_NO"))
})
public class MediaCopy2 {
    private static final Logger log = LoggerFactory.getLogger(MediaCopy2.class);
    @Id //mapped to COPY_NO by attribute override
    private int copyNo;    
    @Id 
    @ManyToOne 
    @JoinColumn(name="MEDIACOPY_MID")
    private Media media;
    
    protected MediaCopy2() { log.debug(super.toString() + ": ctor()"); }
    public MediaCopy2(Media media, int copyNo) {
        log.debug(super.toString() + ": ctor() mediaId="
                + media.getId() + ", copyNo=" + copyNo);
        setMedia(media);
        setCopyNo(copyNo);
    }    

    public int getCopyNo()                { return copyNo; }    
    private void setCopyNo(int copyNo)    { this.copyNo = copyNo; }
    
    public long getMediaId()              { return media.getId();} //mediaId; }
    
    public Media getMedia()               { return media; }    
    private void setMedia(Media media)    { this.media = media; }

    private String myInstance() {
        String s=super.toString();
        s = s.substring(s.lastIndexOf('.')+1);
        return s;
    }

    public String toString() {
        return myInstance() +
            ", mediaId=" + getMediaId() +
            ", copyNo=" + getCopyNo() +
            ", media=" + getMedia();
    }
}
