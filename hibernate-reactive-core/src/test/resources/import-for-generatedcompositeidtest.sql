INSERT INTO fruit_basket(id, name) VALUES (1, 'MyBasket');
ALTER SEQUENCE fruit_basket_seq RESTART WITH 2;

INSERT INTO fruit(id, basket_id, name) VALUES (1, 1, 'Cherry');
INSERT INTO fruit(id, basket_id, name) VALUES (2, 1, 'Apple');
INSERT INTO fruit(id, basket_id, name) VALUES (3, 1, 'Banana');
ALTER SEQUENCE fruit_seq RESTART WITH 4;