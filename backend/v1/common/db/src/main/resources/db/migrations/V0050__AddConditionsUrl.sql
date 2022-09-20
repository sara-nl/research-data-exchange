ALTER TABLE shares ADD COLUMN conditions_url varchar;
UPDATE shares SET conditions_url = 'http://example.com' WHERE conditions_url IS NULL;
ALTER TABLE shares ALTER conditions_url SET NOT NULL;
