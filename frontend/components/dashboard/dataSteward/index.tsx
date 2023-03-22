import React from 'react';
import { AccessLicenseUtil, DatasetsPerPolicy } from '../../../types';
import { Container, Row } from 'react-bootstrap';
import { Table } from 'react-bootstrap';

type Props = {
  datasetsPerPolicy?: Array<DatasetsPerPolicy>
};

const DataStewardDashboard: React.FC<Props> = ({ datasetsPerPolicy }) => {

  return (
    <section>
      <Container>
        <Row className="mt-5">
          <h3>Datasets per Access Policy</h3>
        </Row>
        <Row className="m-5">
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
        </Row>
      </Container>
    </section>
  );
};

export default DataStewardDashboard;
