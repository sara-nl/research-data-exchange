import React from 'react';
import {
    Container,
    Table
} from 'react-bootstrap';
import { Job } from '../../../types';

type Props = {
    jobs?: Array<Job>;
};

const JobOverview: React.FC<Props> = ({ jobs }) => {
    if (jobs.length == 0)
        return <h4>No current or previous analysis jobs found</h4>

    return (
        <section>
            <h3>Current and previous analysis jobs</h3>
            <Table striped bordered hover size="sm">
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Analyst</th>
                        <th>Script</th>
                        <th>Status</th>
                        <th>Results</th>
                    </tr>
                </thead>
                <tbody>
                    {jobs.sort((a, b) => a.id - b.id).map((job, i) => (
                        <tr key={i}>
                            <td>{i + 1}</td>
                            <td>{job.analyst_name} {`<${job.analyst_email}>`}</td>
                            <td><a href={job.script_location}>{job.script_location}</a></td>
                            <td>{job.status.split("_")[0]}</td>
                            <td><a href={job.results_url}>View results</a></td>
                        </tr>
                    ))}
                </tbody>
            </Table>
        </section>
    );
}

export default JobOverview;
