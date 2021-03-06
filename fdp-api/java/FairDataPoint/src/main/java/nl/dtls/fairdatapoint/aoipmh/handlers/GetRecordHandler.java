package nl.dtls.fairdatapoint.aoipmh.handlers;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.dataprovider.exceptions.CannotDisseminateRecordException;
import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.stream.XMLStreamException;
import nl.dtls.fairdatapoint.aoipmh.Item;
import nl.dtls.fairdatapoint.aoipmh.MetadataFormat;
import nl.dtls.fairdatapoint.aoipmh.Repository;
import nl.dtls.fairdatapoint.aoipmh.Set;
import nl.dtls.fairdatapoint.aoipmh.Context;
import nl.dtls.fairdatapoint.aoipmh.writables.About;
import nl.dtls.fairdatapoint.aoipmh.writables.GetRecord;
import nl.dtls.fairdatapoint.aoipmh.writables.Header;
import nl.dtls.fairdatapoint.aoipmh.writables.Metadata;
import nl.dtls.fairdatapoint.aoipmh.writables.Record;
import nl.dtls.fairdatapoint.aoipmh.writables.VerbHandler;
import nl.dtls.fairdatapoint.utils.XmlWriter;
import com.lyncode.xoai.xml.XSLPipeline;
import nl.dtls.fairdatapoint.aoipmh.parameters.OAICompiledRequest;

/**
 * 
 * @author Shamanou van Leeuwen
 * @Since 2016-07-02
 */
public class GetRecordHandler extends VerbHandler<GetRecord> {
    public GetRecordHandler(Context context, Repository repository) {
        super(context, repository);
    }

    @Override
    public GetRecord handle(OAICompiledRequest parameters) throws OAIException, HandlerException {
        Header header = new Header();
        Record record = new Record().withHeader(header);
        GetRecord result = new GetRecord(record);

        MetadataFormat format = getContext().formatForPrefix(parameters.getMetadataPrefix());
        Item item = getRepository().getItemRepository().getItem(parameters.getIdentifier());

        if (getContext().hasCondition() &&
                !getContext().getCondition().getFilter(getRepository().getFilterResolver()).isItemShown(item))
            throw new IdDoesNotExistException("This context does not include this item");

        if (format.hasCondition() &&
                !format.getCondition().getFilter(getRepository().getFilterResolver()).isItemShown(item))
            throw new CannotDisseminateRecordException("Format not applicable to this item");


        header.withIdentifier(item.getIdentifier());
        header.withDatestamp(item.getDatestamp());

        for (Set set : getContext().getSets())
            if (set.getCondition().getFilter(getRepository().getFilterResolver()).isItemShown(item))
                header.withSetSpec(set.getSpec());

        for (Set set : item.getSets())
            header.withSetSpec(set.getSpec());

        if (item.isDeleted())
            header.withStatus(Header.Status.DELETED);

        if (!item.isDeleted()) {
            record.withMetadata(item.getMetadata());

            if (item.getAbout() != null) {
                for (About about : item.getAbout())
                    record.withAbout(about);
            }
        }
        return result;
    }

    private XSLPipeline toPipeline(Item item) throws XmlWriteException, XMLStreamException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter(output);
        Metadata metadata = item.getMetadata();
        metadata.write(writer);
        writer.close();
        return new XSLPipeline(new ByteArrayInputStream(output.toByteArray()), true);
    }
}
