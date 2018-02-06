package dpla.ingestion3.mappers.providers


import java.net.URI

import dpla.ingestion3.model._
import dpla.ingestion3.utils.FlatFileIO
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.util.{Failure, Success}
import scala.xml.XML

class NaraExtractorTest extends FlatSpec with BeforeAndAfter {

  val shortName = "nara"
  val xmlString: String = new FlatFileIO().readFileAsString("/nara.xml")
  val xml = XML.loadString(xmlString)
  val itemUri = new URI("http://catalog.archives.gov/id/2132862")
  val extractor = new NaraExtractor(xmlString, shortName)

  "A NaraExtractor" should "successfully extract from a valid document" in {
    extractor.build() match {
      case Success(data) => succeed
      case Failure(exception) => fail(exception)
    }
  }

  it should "use the provider shortname in minting IDs" in
    assert(extractor.useProviderName())

  it should "pass through the short name to ID minting" in
    assert(extractor.getProviderName() === shortName)

  it should "construct the correct item uri" in
    assert(extractor.itemUri(xml) === itemUri)

  it should "have the correct DPLA ID" in {
    val dplaUri = extractor.build().getOrElse(fail("Extraction failed.")).dplaUri
    assert(dplaUri === new URI("http://dp.la/api/items/805598afebf2c093272a5a044938be59"))
  }

  it should "express the right hub details" in {
    val agent = extractor.agent
    assert(agent.name === Some("National Archives and Records Administration"))
    assert(agent.uri === Some(new URI("http://dp.la/api/contributor/nara")))
  }

  it should "extract collections" in {
    val collections = extractor.collection(xml)
    assert(collections === Seq("Records of the Forest Service"))
  }

  it should "extract contributors" in {
    val contributors = extractor.contributor(xml)
    assert(contributors === Seq("Department of the Navy. Fourteenth Naval District. Naval Air Station, Pearl Harbor (Hawaii). ca. 1940-9/1947"))
  }

  it should "extract creators" in {
    val creators = extractor.creator(xml)
    assert(creators === Seq("Department of Agriculture. Forest Service. Region 9 (Eastern Region). 1965-"))
  }

  //todo better coverage of date possibilities?
  it should "extract dates" in {
    val dates = extractor.date(xml)
    assert(dates === Seq(stringOnlyTimeSpan("1967-10")))
  }

  it should "extract descriptions" in {
    val descriptions = build().sourceResource.description
    assert(descriptions === Seq("Original caption: Aerial view of Silver Island Lake, from inlet, looking north, with Perent Lake in background."))
  }

  it should "extract extents" in {
    val extents = build().sourceResource.extent
    assert(extents === Seq("14 pages"))
  }

  it should "extract formats" in {
    val formats = extractor.format(xml)
    assert(formats === Seq("Aerial views", "Photographic Print"))
  }

  it should "extract identifiers" in {
    val identifiers = build().sourceResource.identifier
    assert(identifiers === Seq("2132862"))
  }

  it should "extract languages" in {
    val languages = build().sourceResource.language
    assert(languages === Seq(nameOnlyConcept("Japanese")))
  }

  it should "extract places" in {
    val places = build().sourceResource.place
    assert(places === Seq(nameOnlyPlace("Superior National Forest (Minn.)")))
  }

  //todo can't find publishers
  it should "extract publishers" in {
    val publishers = extractor.publisher(xml)
    assert(publishers === Seq())
  }

  it should "extract relations" in {
    val relations = extractor.relation(xml)
    assert(relations === Seq("Records of the Forest Service ; Historic Photographs"))
  }

  it should "extract rights" in {
    val rights = extractor.rights(xml)
    //todo this mapping is probably wrong in the way it concatenates values
    assert(rights.head.contains("Unrestricted"))
  }

  it should "extract subjects" in {
    val subjects = build().sourceResource.subject
    assert(subjects === Seq(nameOnlyConcept("Recreation"), nameOnlyConcept("Wilderness areas")))
  }

  it should "extract titles" in {
    val titles = build().sourceResource.title
    assert(titles === Seq("Photograph of Aerial View of Silver Island Lake"))
  }

  it should "extract types" in {
    val types = extractor.types(xml)
    assert(types.head.contains("image"))
  }

  it should "extract dataProviders" in {
    val dataProvider = extractor.dataProvider(xml)
    assert(dataProvider === nameOnlyAgent("National Archives at Chicago"))
  }

  it should "contain the original record" in {
    assert(xmlString === build.originalRecord)
  }

  it should "contain the hub agent as the provider" in {
    assert(
      build().provider === EdmAgent(
        name = Some("National Archives and Records Administration"),
        uri = Some(new URI("http://dp.la/api/contributor/nara"))
      )
    )
  }

  it should "contain the correct isShownAt" in {
    assert(build().isShownAt === uriOnlyWebResource(itemUri))
  }

  //todo should we eliminate these default thumbnails?
  it should "find the item previews" in {
    assert(build().preview === Some(uriOnlyWebResource(new URI("https://nara-media-001.s3.amazonaws.com/arcmedia/great-lakes/001/517805_a.jpg"))))
  }

  it should "extract dataProvider from records with fileUnitPhysicalOccurrence" in {
    val xml = <item><physicalOccurrenceArray>
      <fileUnitPhysicalOccurrence>
        <copyStatus>
          <naId>10031434</naId>
          <termName>Preservation-Reproduction-Reference</termName>
        </copyStatus>
        <copyStatusProposal/>
        <locationArray>
          <location>
            <facility>
              <naId>10048490</naId>
              <termName>National Archives Building - Archives I (Washington, DC)</termName>
            </facility>
            <facilityProposal/>
          </location>
        </locationArray>
        <mediaOccurrenceArray>
          <mediaOccurrence>
            <color/>
            <colorProposal/>
            <dimension/>
            <dimensionProposal/>
            <generalMediaTypeArray>
              <generalMediaType>
                <naId>12000005</naId>
                <termName>Loose Sheets</termName>
              </generalMediaType>
            </generalMediaTypeArray>
            <generalMediaTypeProposalArray/>
            <process/>
            <processProposal/>
            <specificMediaType>
              <naId>10048756</naId>
              <termName>Paper</termName>
            </specificMediaType>
            <specificMediaTypeProposal/>
          </mediaOccurrence>
        </mediaOccurrenceArray>
        <referenceUnitArray>
          <referenceUnit>
            <mailCode>RDT1</mailCode>
            <name>National Archives at Washington, DC - Textual Reference</name>
            <address1>National Archives Building</address1>
            <address2>7th and Pennsylvania Avenue NW</address2>
            <city>Washington</city>
            <state>DC</state>
            <postCode>20408</postCode>
            <phone>202-357-5385</phone>
            <fax>202-357-5936</fax>
            <email>Archives1reference@nara.gov</email>
            <naId>32</naId>
            <termName>National Archives at Washington, DC - Textual Reference</termName>
          </referenceUnit>
        </referenceUnitArray>
        <referenceUnitProposalArray/>
      </fileUnitPhysicalOccurrence>
    </physicalOccurrenceArray></item>

    println(extractor.dataProvider(xml))

  }



  def build(): OreAggregation = extractor
    .build()
    .getOrElse(fail("Extraction failed."))
}