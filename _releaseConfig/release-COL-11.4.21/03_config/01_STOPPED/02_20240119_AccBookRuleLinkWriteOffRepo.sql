delete from acc_book_rule_link
where product_type ='Repo' and acc_rule_id is null;

--- Disponible para la venta ---

delete  from acc_book_rule_link
where product_type ='Repo' 
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Disponible para la venta');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 1 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Disponible para la venta'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Disponible para la venta');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 2 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Disponible para la venta'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting'),
'isWriteOffNetting',0);

delete  from acc_book_rule_link
where product_type ='Repo' 
--
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Disponible para la venta');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 3 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Disponible para la venta'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Disponible para la venta');


insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 4 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Disponible para la venta'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD'),
'isWriteOffNetting',0);

--- Inversion a vencimiento ---

delete  from acc_book_rule_link
where product_type ='Repo' 
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Inversion a vencimiento');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 9 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Inversion a vencimiento'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Inversion a vencimiento');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 10 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Inversion a vencimiento'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting'),
'isWriteOffNetting',0);

delete  from acc_book_rule_link
where product_type ='Repo' 
--
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Inversion a vencimiento');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 11 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Inversion a vencimiento'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Inversion a vencimiento');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 12 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Inversion a vencimiento'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD'),
'isWriteOffNetting',0);

--- Inversion crediticia ---

delete  from acc_book_rule_link
where product_type ='Repo' 
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Inversion crediticia');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 13 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Inversion crediticia'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Inversion crediticia');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 14 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Inversion crediticia'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting'),
'isWriteOffNetting',0);

delete  from acc_book_rule_link
where product_type ='Repo' 
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Inversion crediticia');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 15 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Inversion crediticia'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Inversion crediticia');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 16 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Inversion crediticia'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD'),
'isWriteOffNetting',0);

--- Negociacion ---

delete  from acc_book_rule_link
where product_type ='Repo' 
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Negociacion');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 17 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Negociacion'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Negociacion');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 18 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Negociacion'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting'),
'isWriteOffNetting',0);

delete  from acc_book_rule_link
where product_type ='Repo' 
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Negociacion');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 19 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Negociacion'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Negociacion');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 20 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Negociacion'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD'),
'isWriteOffNetting',0);


--- Otros a valor razonable ---

delete  from acc_book_rule_link
where product_type ='Repo' 
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Otros a valor razonable');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 21 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Otros a valor razonable'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Otros a valor razonable');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 22 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Otros a valor razonable'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting'),
'isWriteOffNetting',0);

delete  from acc_book_rule_link
where product_type ='Repo' 
--and sd_filter='isWriteOffNotNetted'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Otros a valor razonable');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 23 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Otros a valor razonable'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOff_BDSD'),
'isWriteOffNotNetted',0);

delete  from acc_book_rule_link
where product_type ='Repo' and sd_filter='isWriteOffNetting'
and acc_rule_id = (select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD')
and acc_book_id = (select acc_book_id from acc_book where acc_book_name='Otros a valor razonable');

insert into acc_book_rule_link
(acc_link_id, acc_book_id, product_type, acc_rule_id, sd_filter, version_num) values(
(select last_id + 24 from calypso_seed where seed_name = 'refdata'),
(select acc_book_id from acc_book where acc_book_name='Otros a valor razonable'),
'Repo',
(select acc_rule_id from acc_rule where acc_rule_name='Repo_WriteOffNetting_BDSD'),
'isWriteOffNetting',0);


--Disponible para la venta
--Inventario Terceros = not requred 
--Inversion a vencimiento
--Inversion crediticia
--Negociacion
--NONE
--Otros a valor razonable


update calypso_seed set last_id = last_id + 30 where seed_name = 'refdata'; 

commit;