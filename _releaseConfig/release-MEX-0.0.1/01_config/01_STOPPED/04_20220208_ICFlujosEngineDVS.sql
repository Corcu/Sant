delete from domain_values where name = 'domainName' and value = 'BondIgnoreChanges';
Insert into DOMAIN_VALUES (NAME, VALUE, DESCRIPTION) values ('domainName', 'BondIgnoreChanges', '');

delete from domain_values where name = 'BondIgnoreChanges' and value = '__secCodes';
Insert into DOMAIN_VALUES (NAME, VALUE, DESCRIPTION) values ('BondIgnoreChanges', '__secCodes', '');

delete from domain_values where name = 'messageType' and value = 'BOND_IC_EXPORT';
Insert into DOMAIN_VALUES (NAME, VALUE, DESCRIPTION) values ('messageType', 'BOND_IC_EXPORT', '');

delete from domain_values where name = 'eventClass' and value = 'PSEventProduct';
Insert into DOMAIN_VALUES (NAME, VALUE, DESCRIPTION) values ('eventClass', 'PSEventProduct', '');

delete from domain_values where name = 'eventFilter' and value = 'BondDefICExportAckEngineEventFilter';
Insert into DOMAIN_VALUES (NAME, VALUE, DESCRIPTION) values ('eventFilter', 'BondDefICExportAckEngineEventFilter', '');
