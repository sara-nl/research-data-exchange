CREATE TABLE Shares(
  id serial PRIMARY KEY,
  metadata json NOT NULL,
  preview json NOT NULL,
  created_at TIMESTAMP NOT NULL,
  token uuid UNIQUE NOT NULL,
  token_expires_at TIMESTAMP NOT NULL,
  published_at TIMESTAMP
)