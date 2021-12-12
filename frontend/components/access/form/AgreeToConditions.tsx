import { Form as RForm } from 'react-bootstrap';
import { FieldProps } from 'formik';

import React from 'react';

type Props = {
  formikFieldProps: FieldProps;
  disabled: boolean;
};

const AgreeToConditions: React.FC<Props> = ({ formikFieldProps, disabled }) => (
  <span className="d-inline-block">
    <RForm.Check
      type="checkbox"
      required
      label={
        <React.Fragment>
          <span className="lead">
            I hereby agree to the terms and conditions{' '}
          </span>
          <sup className="font-weight-light">required</sup>
        </React.Fragment>
      }
      disabled={disabled}
      value={formikFieldProps.field.value}
      onBlur={formikFieldProps.field.onBlur}
      onChange={formikFieldProps.field.onChange}
    />
  </span>
);

export default AgreeToConditions;
