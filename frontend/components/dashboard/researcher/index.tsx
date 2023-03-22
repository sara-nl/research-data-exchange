import React from 'react';
import { AccessLicense, AccessLicenseUtil, DatasetStats } from '../../../types';
import { Container, Row } from 'react-bootstrap';
import { Col } from 'react-bootstrap';

type Props = {
  datasetStatsArray?: Array<DatasetStats>
};

const ResearcherDashboard: React.FC<Props> = ({ datasetStatsArray }) => {

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
              <p><b>Signed: </b>{datasetStats.signed} times {(datasetStats.access_license_id != AccessLicense.download) && (<span>/ <b>Analyzed: </b>{datasetStats.analyzed} times</span>)}</p>

            </Col>
          </Row>
        ))}
        {/* <Row className="m-5">
          <Table striped bordered hover size="sm">
            <thead>
              <tr>
                <th>Access policy</th>
                <th>Number of datasets</th>
              </tr>
            </thead>
            <tbody>
              {datasetsPerPolicy.map((dpp, i) => (
                <tr key={i}>
                  <td>{AccessLicenseUtil.toString(dpp.access_license_id)}</td>
                  <td>{dpp.total}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        </Row> */}
      </Container>
    </section>
  );
};

export default ResearcherDashboard;
