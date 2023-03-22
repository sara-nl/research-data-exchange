import React from 'react';
import { AccessLicense, AccessLicenseUtil, DatasetStats } from '../../../types';
import { Container, Row } from 'react-bootstrap';
import { Col } from 'react-bootstrap';

type Props = {
  datasetStatsArray?: Array<DatasetStats>
  token: string
};

const ResearcherDashboard: React.FC<Props> = ({ datasetStatsArray, token }) => {

  return (
    <section>
      <Container>
        <Row className="mt-5">
          <h3>Datasets per Access Policy</h3>
        </Row>
        {datasetStatsArray.map((datasetStats, i) => (
          <Row key={i} className="mt-5">
            <Col>
              <h4>{datasetStats.doi}</h4>
              <p><i>{datasetStats.title}</i></p>
              <p><b>Policy: </b>{AccessLicenseUtil.toString(datasetStats.access_license_id)}</p>
              <p><b>Signed: </b>{datasetStats.signed} times {(datasetStats.access_license_id != AccessLicense.download) && (<span>/ <b>Analyzed: </b><a href={`/dashboard/dataset/${datasetStats.id}?token=${token}`}>{datasetStats.analyzed} times</a></span>)}</p>
            </Col>
          </Row>
        ))}
      </Container>
    </section>
  );
};

export default ResearcherDashboard;
