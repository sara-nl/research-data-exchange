import React from 'react';
import {
    Container,
    Table
} from 'react-bootstrap';
import { Job } from '../../types';

type Props = {
    jobs?: Array<Job>;
};

const JobOverview: React.FC<Props> = ({ jobs }) => {
    if (jobs.length == 0)
        return <h4>No current or previous analysis jobs found</h4>

    return (
        <section>
            <h4>Current and previous analysis jobs</h4>
            <Table striped bordered hover size="sm">
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Script</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    {jobs.sort((a, b) => a.id - b.id).map((job, i) => (
                        <tr key={i}>
                            <td>{i + 1}</td>
                            <td><a href={job.script_location}>{job.script_location}</a></td>
                            <td>{job.status.split("_")[0]}</td>
                        </tr>
                    ))}
                </tbody>
            </Table>
        </section>
    );
}

export default JobOverview;
