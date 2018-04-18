/* Insert client */
INSERT INTO clients (name, street, city, state, postal_code, 
                     contact_last_name, contact_first_name, contact_middle_name)
       VALUES ('Acme Industries', '1616 Index Ct.', 'Redmond', 'WA', '98055',
               'Coyote', 'Wiley', 'E');

/* Insert consultant */
INSERT INTO consultants (last_name, first_name, middle_name)
       VALUES ('Architect', 'Ann', 'S.');

/* Select consultant id */
SELECT id 
  FROM consultants 
 WHERE last_name = 'Architect' 
   AND first_name = 'Ann' 
   AND middle_name = 'S.';

/* Insert time card */
INSERT INTO timecards (consultant_id, start_date)
       VALUES (1, '2005/03/01');

/* Insert non-billable hours */
INSERT INTO non_billable_hours (account_name, timecard_id, date, hours)
       VALUES ('VACATION', 1, '2005/03/13', 8);

/* Insert billable hours */
INSERT INTO billable_hours (client_id, timecard_id, date, skill, hours)
       VALUES ((SELECT DISTINCT id
                  FROM clients
                 WHERE name = 'Acme Industries'),
                       3, '2005/03/12', 'Software Engineer', 8);

/* Select invoice items */
SELECT b.date, c.last_name, c.first_name, c.middle_name,
       b.skill, s.rate, b.hours
  FROM billable_hours b, consultants c, skills s, timecards t
 WHERE b.client_id = (SELECT DISTINCT id
                        FROM clients
                        WHERE name = 'Acme Industries')
   AND b.skill = s.name
   AND b.timecard_id = t.id
   AND c.id = t.consultant_id
   AND b.date >= '2005/03/01'
   AND b.date <= '2005/03/31';

/* Select all clients */
SELECT name, street, city, state, postal_code,
      contact_last_name, contact_first_name, contact_middle_name
  FROM clients;

/* Select all consultants */
SELECT last_name, first_name, middle_name 
  FROM consultants;

