PGDMP     (                     }            cs    12.22    12.22 j    �           0    0    ENCODING    ENCODING        SET client_encoding = 'UTF8';
                      false            �           0    0 
   STDSTRINGS 
   STDSTRINGS     (   SET standard_conforming_strings = 'on';
                      false            �           0    0 
   SEARCHPATH 
   SEARCHPATH     8   SELECT pg_catalog.set_config('search_path', '', false);
                      false            �           1262    43047    cs    DATABASE     �   CREATE DATABASE cs WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'English_India.1252' LC_CTYPE = 'English_India.1252';
    DROP DATABASE cs;
                postgres    false                        2615    2200    public    SCHEMA        CREATE SCHEMA public;
    DROP SCHEMA public;
                postgres    false            �           0    0    SCHEMA public    COMMENT     6   COMMENT ON SCHEMA public IS 'standard public schema';
                   postgres    false    3            �            1259    43319    category    TABLE     O  CREATE TABLE public.category (
    id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    name character varying(256) NOT NULL,
    remaining_quantity bigint DEFAULT 0,
    status character varying(2) NOT NULL,
    updated_at timestamp with time zone,
    client_id bigint,
    created_by bigint
);
    DROP TABLE public.category;
       public         heap    postgres    false    3            �            1259    43317    category_id_seq    SEQUENCE     x   CREATE SEQUENCE public.category_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 &   DROP SEQUENCE public.category_id_seq;
       public          postgres    false    3    203            �           0    0    category_id_seq    SEQUENCE OWNED BY     C   ALTER SEQUENCE public.category_id_seq OWNED BY public.category.id;
          public          postgres    false    202            �            1259    43329    client    TABLE     ^  CREATE TABLE public.client (
    id bigint NOT NULL,
    address character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    email character varying(256),
    name character varying(256) NOT NULL,
    phone character varying(32),
    status character varying(2) NOT NULL,
    updated_at timestamp with time zone
);
    DROP TABLE public.client;
       public         heap    postgres    false    3            �            1259    43327    client_id_seq    SEQUENCE     v   CREATE SEQUENCE public.client_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 $   DROP SEQUENCE public.client_id_seq;
       public          postgres    false    3    205            �           0    0    client_id_seq    SEQUENCE OWNED BY     ?   ALTER SEQUENCE public.client_id_seq OWNED BY public.client.id;
          public          postgres    false    204            �            1259    43341    customer    TABLE     V  CREATE TABLE public.customer (
    id bigint NOT NULL,
    address character varying(512),
    coating_unit_price numeric(19,2),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    email character varying(256),
    gst character varying(15),
    mobile character varying(15),
    name character varying(256) NOT NULL,
    next_action_date timestamp(6) with time zone,
    remaining_payment_amount numeric(19,2),
    remarks character varying(1000),
    status character varying(2) NOT NULL,
    updated_at timestamp with time zone,
    client_id bigint,
    created_by bigint
);
    DROP TABLE public.customer;
       public         heap    postgres    false    3            �            1259    43339    customer_id_seq    SEQUENCE     x   CREATE SEQUENCE public.customer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 &   DROP SEQUENCE public.customer_id_seq;
       public          postgres    false    3    207            �           0    0    customer_id_seq    SEQUENCE OWNED BY     C   ALTER SEQUENCE public.customer_id_seq OWNED BY public.customer.id;
          public          postgres    false    206            �            1259    43353    employee    TABLE     �  CREATE TABLE public.employee (
    id bigint NOT NULL,
    address character varying(512),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    department character varying(100),
    designation character varying(100),
    email character varying(256),
    mobile_number character varying(15),
    name character varying(256) NOT NULL,
    status character varying(2) NOT NULL,
    updated_at timestamp with time zone,
    client_id bigint,
    created_by bigint
);
    DROP TABLE public.employee;
       public         heap    postgres    false    3            �            1259    43351    employee_id_seq    SEQUENCE     x   CREATE SEQUENCE public.employee_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 &   DROP SEQUENCE public.employee_id_seq;
       public          postgres    false    3    209            �           0    0    employee_id_seq    SEQUENCE OWNED BY     C   ALTER SEQUENCE public.employee_id_seq OWNED BY public.employee.id;
          public          postgres    false    208            �            1259    43365    product    TABLE     [  CREATE TABLE public.product (
    id bigint NOT NULL,
    blocked_quantity bigint DEFAULT 0,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    description character varying(255),
    minimum_stock numeric(19,2),
    name character varying(256) NOT NULL,
    purchase_amount numeric(19,2) DEFAULT 0,
    remaining_quantity bigint DEFAULT 0,
    sale_amount numeric(19,2) DEFAULT 0,
    status character varying(2) NOT NULL,
    total_remaining_quantity bigint DEFAULT 0,
    updated_at timestamp with time zone,
    category_id bigint,
    client_id bigint,
    created_by bigint
);
    DROP TABLE public.product;
       public         heap    postgres    false    3            �            1259    43363    product_id_seq    SEQUENCE     w   CREATE SEQUENCE public.product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 %   DROP SEQUENCE public.product_id_seq;
       public          postgres    false    3    211            �           0    0    product_id_seq    SEQUENCE OWNED BY     A   ALTER SEQUENCE public.product_id_seq OWNED BY public.product.id;
          public          postgres    false    210            �            1259    43382    purchase    TABLE     �  CREATE TABLE public.purchase (
    id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    invoice_number character varying(255),
    purchase_date timestamp(6) without time zone NOT NULL,
    total_purchase_amount numeric(19,2) DEFAULT 0,
    updated_at timestamp with time zone NOT NULL,
    client_id bigint NOT NULL,
    created_by bigint,
    customer_id bigint,
    updated_by bigint,
    number_of_items integer
);
    DROP TABLE public.purchase;
       public         heap    postgres    false    3            �            1259    43380    purchase_id_seq    SEQUENCE     x   CREATE SEQUENCE public.purchase_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 &   DROP SEQUENCE public.purchase_id_seq;
       public          postgres    false    3    213            �           0    0    purchase_id_seq    SEQUENCE OWNED BY     C   ALTER SEQUENCE public.purchase_id_seq OWNED BY public.purchase.id;
          public          postgres    false    212            �            1259    43392    purchase_items    TABLE     �  CREATE TABLE public.purchase_items (
    id bigint NOT NULL,
    discount_amount numeric(19,2) DEFAULT 0,
    discount_percentage numeric(6,4) DEFAULT 0,
    final_price numeric(19,2) NOT NULL,
    quantity bigint NOT NULL,
    remaining_quantity bigint DEFAULT 0,
    unit_price numeric(19,2) NOT NULL,
    client_id bigint NOT NULL,
    product_id bigint NOT NULL,
    purchase_id bigint NOT NULL
);
 "   DROP TABLE public.purchase_items;
       public         heap    postgres    false    3            �            1259    43390    purchase_items_id_seq    SEQUENCE     ~   CREATE SEQUENCE public.purchase_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 ,   DROP SEQUENCE public.purchase_items_id_seq;
       public          postgres    false    3    215            �           0    0    purchase_items_id_seq    SEQUENCE OWNED BY     O   ALTER SEQUENCE public.purchase_items_id_seq OWNED BY public.purchase_items.id;
          public          postgres    false    214            �            1259    43403    user_master    TABLE     Y  CREATE TABLE public.user_master (
    id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    email character varying(64),
    fail_login_count integer NOT NULL,
    first_name character varying(64),
    jwt_token character varying(256),
    last_name character varying(64),
    lock_time timestamp(6) with time zone,
    password character varying(256) NOT NULL,
    refresh_token character varying(64),
    refresh_token_expiry timestamp(6) with time zone,
    status character varying(2) NOT NULL,
    updated_at timestamp with time zone,
    client_id bigint
);
    DROP TABLE public.user_master;
       public         heap    postgres    false    3            �            1259    43401    user_master_id_seq    SEQUENCE     {   CREATE SEQUENCE public.user_master_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 )   DROP SEQUENCE public.user_master_id_seq;
       public          postgres    false    3    217            �           0    0    user_master_id_seq    SEQUENCE OWNED BY     I   ALTER SEQUENCE public.user_master_id_seq OWNED BY public.user_master.id;
          public          postgres    false    216            �
           2604    43322    category id    DEFAULT     j   ALTER TABLE ONLY public.category ALTER COLUMN id SET DEFAULT nextval('public.category_id_seq'::regclass);
 :   ALTER TABLE public.category ALTER COLUMN id DROP DEFAULT;
       public          postgres    false    202    203    203            �
           2604    43332 	   client id    DEFAULT     f   ALTER TABLE ONLY public.client ALTER COLUMN id SET DEFAULT nextval('public.client_id_seq'::regclass);
 8   ALTER TABLE public.client ALTER COLUMN id DROP DEFAULT;
       public          postgres    false    204    205    205            �
           2604    43344    customer id    DEFAULT     j   ALTER TABLE ONLY public.customer ALTER COLUMN id SET DEFAULT nextval('public.customer_id_seq'::regclass);
 :   ALTER TABLE public.customer ALTER COLUMN id DROP DEFAULT;
       public          postgres    false    207    206    207            �
           2604    43356    employee id    DEFAULT     j   ALTER TABLE ONLY public.employee ALTER COLUMN id SET DEFAULT nextval('public.employee_id_seq'::regclass);
 :   ALTER TABLE public.employee ALTER COLUMN id DROP DEFAULT;
       public          postgres    false    209    208    209            �
           2604    43368 
   product id    DEFAULT     h   ALTER TABLE ONLY public.product ALTER COLUMN id SET DEFAULT nextval('public.product_id_seq'::regclass);
 9   ALTER TABLE public.product ALTER COLUMN id DROP DEFAULT;
       public          postgres    false    210    211    211            �
           2604    43385    purchase id    DEFAULT     j   ALTER TABLE ONLY public.purchase ALTER COLUMN id SET DEFAULT nextval('public.purchase_id_seq'::regclass);
 :   ALTER TABLE public.purchase ALTER COLUMN id DROP DEFAULT;
       public          postgres    false    212    213    213            �
           2604    43395    purchase_items id    DEFAULT     v   ALTER TABLE ONLY public.purchase_items ALTER COLUMN id SET DEFAULT nextval('public.purchase_items_id_seq'::regclass);
 @   ALTER TABLE public.purchase_items ALTER COLUMN id DROP DEFAULT;
       public          postgres    false    215    214    215            �
           2604    43406    user_master id    DEFAULT     p   ALTER TABLE ONLY public.user_master ALTER COLUMN id SET DEFAULT nextval('public.user_master_id_seq'::regclass);
 =   ALTER TABLE public.user_master ALTER COLUMN id DROP DEFAULT;
       public          postgres    false    217    216    217            �          0    43319    category 
   TABLE DATA           w   COPY public.category (id, created_at, name, remaining_quantity, status, updated_at, client_id, created_by) FROM stdin;
    public          postgres    false    203   ۇ       �          0    43329    client 
   TABLE DATA           a   COPY public.client (id, address, created_at, email, name, phone, status, updated_at) FROM stdin;
    public          postgres    false    205   ]�       �          0    43341    customer 
   TABLE DATA           �   COPY public.customer (id, address, coating_unit_price, created_at, email, gst, mobile, name, next_action_date, remaining_payment_amount, remarks, status, updated_at, client_id, created_by) FROM stdin;
    public          postgres    false    207   ��       �          0    43353    employee 
   TABLE DATA           �   COPY public.employee (id, address, created_at, department, designation, email, mobile_number, name, status, updated_at, client_id, created_by) FROM stdin;
    public          postgres    false    209   f�       �          0    43365    product 
   TABLE DATA           �   COPY public.product (id, blocked_quantity, created_at, description, minimum_stock, name, purchase_amount, remaining_quantity, sale_amount, status, total_remaining_quantity, updated_at, category_id, client_id, created_by) FROM stdin;
    public          postgres    false    211   ��       �          0    43382    purchase 
   TABLE DATA           �   COPY public.purchase (id, created_at, invoice_number, purchase_date, total_purchase_amount, updated_at, client_id, created_by, customer_id, updated_by, number_of_items) FROM stdin;
    public          postgres    false    213   ��       �          0    43392    purchase_items 
   TABLE DATA           �   COPY public.purchase_items (id, discount_amount, discount_percentage, final_price, quantity, remaining_quantity, unit_price, client_id, product_id, purchase_id) FROM stdin;
    public          postgres    false    215   �       �          0    43403    user_master 
   TABLE DATA           �   COPY public.user_master (id, created_at, email, fail_login_count, first_name, jwt_token, last_name, lock_time, password, refresh_token, refresh_token_expiry, status, updated_at, client_id) FROM stdin;
    public          postgres    false    217   x�       �           0    0    category_id_seq    SEQUENCE SET     =   SELECT pg_catalog.setval('public.category_id_seq', 4, true);
          public          postgres    false    202            �           0    0    client_id_seq    SEQUENCE SET     ;   SELECT pg_catalog.setval('public.client_id_seq', 2, true);
          public          postgres    false    204            �           0    0    customer_id_seq    SEQUENCE SET     =   SELECT pg_catalog.setval('public.customer_id_seq', 4, true);
          public          postgres    false    206            �           0    0    employee_id_seq    SEQUENCE SET     >   SELECT pg_catalog.setval('public.employee_id_seq', 1, false);
          public          postgres    false    208            �           0    0    product_id_seq    SEQUENCE SET     <   SELECT pg_catalog.setval('public.product_id_seq', 9, true);
          public          postgres    false    210            �           0    0    purchase_id_seq    SEQUENCE SET     =   SELECT pg_catalog.setval('public.purchase_id_seq', 7, true);
          public          postgres    false    212            �           0    0    purchase_items_id_seq    SEQUENCE SET     D   SELECT pg_catalog.setval('public.purchase_items_id_seq', 17, true);
          public          postgres    false    214            �           0    0    user_master_id_seq    SEQUENCE SET     @   SELECT pg_catalog.setval('public.user_master_id_seq', 3, true);
          public          postgres    false    216            �
           2606    43326    category category_pkey 
   CONSTRAINT     T   ALTER TABLE ONLY public.category
    ADD CONSTRAINT category_pkey PRIMARY KEY (id);
 @   ALTER TABLE ONLY public.category DROP CONSTRAINT category_pkey;
       public            postgres    false    203            �
           2606    43338    client client_pkey 
   CONSTRAINT     P   ALTER TABLE ONLY public.client
    ADD CONSTRAINT client_pkey PRIMARY KEY (id);
 <   ALTER TABLE ONLY public.client DROP CONSTRAINT client_pkey;
       public            postgres    false    205            �
           2606    43350    customer customer_pkey 
   CONSTRAINT     T   ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id);
 @   ALTER TABLE ONLY public.customer DROP CONSTRAINT customer_pkey;
       public            postgres    false    207            �
           2606    43362    employee employee_pkey 
   CONSTRAINT     T   ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_pkey PRIMARY KEY (id);
 @   ALTER TABLE ONLY public.employee DROP CONSTRAINT employee_pkey;
       public            postgres    false    209            �
           2606    43379    product product_pkey 
   CONSTRAINT     R   ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);
 >   ALTER TABLE ONLY public.product DROP CONSTRAINT product_pkey;
       public            postgres    false    211            �
           2606    43400 "   purchase_items purchase_items_pkey 
   CONSTRAINT     `   ALTER TABLE ONLY public.purchase_items
    ADD CONSTRAINT purchase_items_pkey PRIMARY KEY (id);
 L   ALTER TABLE ONLY public.purchase_items DROP CONSTRAINT purchase_items_pkey;
       public            postgres    false    215            �
           2606    43389    purchase purchase_pkey 
   CONSTRAINT     T   ALTER TABLE ONLY public.purchase
    ADD CONSTRAINT purchase_pkey PRIMARY KEY (id);
 @   ALTER TABLE ONLY public.purchase DROP CONSTRAINT purchase_pkey;
       public            postgres    false    213            �
           2606    43416    category uk_category_name 
   CONSTRAINT     T   ALTER TABLE ONLY public.category
    ADD CONSTRAINT uk_category_name UNIQUE (name);
 C   ALTER TABLE ONLY public.category DROP CONSTRAINT uk_category_name;
       public            postgres    false    203            �
           2606    43432     product uk_product_category_name 
   CONSTRAINT     h   ALTER TABLE ONLY public.product
    ADD CONSTRAINT uk_product_category_name UNIQUE (category_id, name);
 J   ALTER TABLE ONLY public.product DROP CONSTRAINT uk_product_category_name;
       public            postgres    false    211    211            �
           2606    43442     user_master uk_user_master_email 
   CONSTRAINT     \   ALTER TABLE ONLY public.user_master
    ADD CONSTRAINT uk_user_master_email UNIQUE (email);
 J   ALTER TABLE ONLY public.user_master DROP CONSTRAINT uk_user_master_email;
       public            postgres    false    217            �
           2606    43412    user_master user_master_pkey 
   CONSTRAINT     Z   ALTER TABLE ONLY public.user_master
    ADD CONSTRAINT user_master_pkey PRIMARY KEY (id);
 F   ALTER TABLE ONLY public.user_master DROP CONSTRAINT user_master_pkey;
       public            postgres    false    217            �
           1259    43414    idx_category_client_id    INDEX     P   CREATE INDEX idx_category_client_id ON public.category USING btree (client_id);
 *   DROP INDEX public.idx_category_client_id;
       public            postgres    false    203            �
           1259    43413    idx_category_name    INDEX     F   CREATE INDEX idx_category_name ON public.category USING btree (name);
 %   DROP INDEX public.idx_category_name;
       public            postgres    false    203            �
           1259    43421    idx_customer_client_id    INDEX     P   CREATE INDEX idx_customer_client_id ON public.customer USING btree (client_id);
 *   DROP INDEX public.idx_customer_client_id;
       public            postgres    false    207            �
           1259    43419    idx_customer_email    INDEX     H   CREATE INDEX idx_customer_email ON public.customer USING btree (email);
 &   DROP INDEX public.idx_customer_email;
       public            postgres    false    207            �
           1259    43418    idx_customer_mobile    INDEX     J   CREATE INDEX idx_customer_mobile ON public.customer USING btree (mobile);
 '   DROP INDEX public.idx_customer_mobile;
       public            postgres    false    207            �
           1259    43417    idx_customer_name    INDEX     F   CREATE INDEX idx_customer_name ON public.customer USING btree (name);
 %   DROP INDEX public.idx_customer_name;
       public            postgres    false    207            �
           1259    43420 %   idx_customer_remaining_payment_amount    INDEX     n   CREATE INDEX idx_customer_remaining_payment_amount ON public.customer USING btree (remaining_payment_amount);
 9   DROP INDEX public.idx_customer_remaining_payment_amount;
       public            postgres    false    207            �
           1259    43425    idx_employee_client_id    INDEX     P   CREATE INDEX idx_employee_client_id ON public.employee USING btree (client_id);
 *   DROP INDEX public.idx_employee_client_id;
       public            postgres    false    209            �
           1259    43423    idx_employee_mobile    INDEX     Q   CREATE INDEX idx_employee_mobile ON public.employee USING btree (mobile_number);
 '   DROP INDEX public.idx_employee_mobile;
       public            postgres    false    209            �
           1259    43422    idx_employee_name    INDEX     F   CREATE INDEX idx_employee_name ON public.employee USING btree (name);
 %   DROP INDEX public.idx_employee_name;
       public            postgres    false    209            �
           1259    43424    idx_employee_status    INDEX     J   CREATE INDEX idx_employee_status ON public.employee USING btree (status);
 '   DROP INDEX public.idx_employee_status;
       public            postgres    false    209            �
           1259    43429    idx_product_category_id    INDEX     R   CREATE INDEX idx_product_category_id ON public.product USING btree (category_id);
 +   DROP INDEX public.idx_product_category_id;
       public            postgres    false    211            �
           1259    43430    idx_product_client_id    INDEX     N   CREATE INDEX idx_product_client_id ON public.product USING btree (client_id);
 )   DROP INDEX public.idx_product_client_id;
       public            postgres    false    211            �
           1259    43426    idx_product_name    INDEX     D   CREATE INDEX idx_product_name ON public.product USING btree (name);
 $   DROP INDEX public.idx_product_name;
       public            postgres    false    211            �
           1259    43428    idx_product_remaining_quantity    INDEX     `   CREATE INDEX idx_product_remaining_quantity ON public.product USING btree (remaining_quantity);
 2   DROP INDEX public.idx_product_remaining_quantity;
       public            postgres    false    211            �
           1259    43427    idx_product_status    INDEX     H   CREATE INDEX idx_product_status ON public.product USING btree (status);
 &   DROP INDEX public.idx_product_status;
       public            postgres    false    211            �
           1259    43436    idx_purchase_client_id    INDEX     P   CREATE INDEX idx_purchase_client_id ON public.purchase USING btree (client_id);
 *   DROP INDEX public.idx_purchase_client_id;
       public            postgres    false    213            �
           1259    43435    idx_purchase_customer_id    INDEX     T   CREATE INDEX idx_purchase_customer_id ON public.purchase USING btree (customer_id);
 ,   DROP INDEX public.idx_purchase_customer_id;
       public            postgres    false    213            �
           1259    43433    idx_purchase_invoice_number    INDEX     Z   CREATE INDEX idx_purchase_invoice_number ON public.purchase USING btree (invoice_number);
 /   DROP INDEX public.idx_purchase_invoice_number;
       public            postgres    false    213            �
           1259    43438    idx_purchase_items_product_id    INDEX     ^   CREATE INDEX idx_purchase_items_product_id ON public.purchase_items USING btree (product_id);
 1   DROP INDEX public.idx_purchase_items_product_id;
       public            postgres    false    215            �
           1259    43437    idx_purchase_items_purchase_id    INDEX     `   CREATE INDEX idx_purchase_items_purchase_id ON public.purchase_items USING btree (purchase_id);
 2   DROP INDEX public.idx_purchase_items_purchase_id;
       public            postgres    false    215            �
           1259    43434    idx_purchase_purchase_date    INDEX     X   CREATE INDEX idx_purchase_purchase_date ON public.purchase USING btree (purchase_date);
 .   DROP INDEX public.idx_purchase_purchase_date;
       public            postgres    false    213            �
           1259    43440    idx_user_master_client_id    INDEX     V   CREATE INDEX idx_user_master_client_id ON public.user_master USING btree (client_id);
 -   DROP INDEX public.idx_user_master_client_id;
       public            postgres    false    217            �
           1259    43439    idx_user_master_email    INDEX     N   CREATE INDEX idx_user_master_email ON public.user_master USING btree (email);
 )   DROP INDEX public.idx_user_master_email;
       public            postgres    false    217            �
           2606    43443 (   category fk_category_client_id_client_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.category
    ADD CONSTRAINT fk_category_client_id_client_id FOREIGN KEY (client_id) REFERENCES public.client(id);
 R   ALTER TABLE ONLY public.category DROP CONSTRAINT fk_category_client_id_client_id;
       public          postgres    false    205    203    2766            �
           2606    43448 .   category fk_category_created_by_user_master_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.category
    ADD CONSTRAINT fk_category_created_by_user_master_id FOREIGN KEY (created_by) REFERENCES public.user_master(id);
 X   ALTER TABLE ONLY public.category DROP CONSTRAINT fk_category_created_by_user_master_id;
       public          postgres    false    203    2804    217            �
           2606    43453 (   customer fk_customer_client_id_client_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.customer
    ADD CONSTRAINT fk_customer_client_id_client_id FOREIGN KEY (client_id) REFERENCES public.client(id);
 R   ALTER TABLE ONLY public.customer DROP CONSTRAINT fk_customer_client_id_client_id;
       public          postgres    false    2766    205    207            �
           2606    43458 .   customer fk_customer_created_by_user_master_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.customer
    ADD CONSTRAINT fk_customer_created_by_user_master_id FOREIGN KEY (created_by) REFERENCES public.user_master(id);
 X   ALTER TABLE ONLY public.customer DROP CONSTRAINT fk_customer_created_by_user_master_id;
       public          postgres    false    2804    217    207            �
           2606    43463 (   employee fk_employee_client_id_client_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.employee
    ADD CONSTRAINT fk_employee_client_id_client_id FOREIGN KEY (client_id) REFERENCES public.client(id);
 R   ALTER TABLE ONLY public.employee DROP CONSTRAINT fk_employee_client_id_client_id;
       public          postgres    false    2766    205    209            �
           2606    43468 .   employee fk_employee_created_by_user_master_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.employee
    ADD CONSTRAINT fk_employee_created_by_user_master_id FOREIGN KEY (created_by) REFERENCES public.user_master(id);
 X   ALTER TABLE ONLY public.employee DROP CONSTRAINT fk_employee_created_by_user_master_id;
       public          postgres    false    217    209    2804            �
           2606    43473 *   product fk_product_category_id_category_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.product
    ADD CONSTRAINT fk_product_category_id_category_id FOREIGN KEY (category_id) REFERENCES public.category(id);
 T   ALTER TABLE ONLY public.product DROP CONSTRAINT fk_product_category_id_category_id;
       public          postgres    false    203    211    2760            �
           2606    43478 &   product fk_product_client_id_client_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.product
    ADD CONSTRAINT fk_product_client_id_client_id FOREIGN KEY (client_id) REFERENCES public.client(id);
 P   ALTER TABLE ONLY public.product DROP CONSTRAINT fk_product_client_id_client_id;
       public          postgres    false    205    211    2766            �
           2606    43483 ,   product fk_product_created_by_user_master_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.product
    ADD CONSTRAINT fk_product_created_by_user_master_id FOREIGN KEY (created_by) REFERENCES public.user_master(id);
 V   ALTER TABLE ONLY public.product DROP CONSTRAINT fk_product_created_by_user_master_id;
       public          postgres    false    211    2804    217            �
           2606    43488 (   purchase fk_purchase_client_id_client_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.purchase
    ADD CONSTRAINT fk_purchase_client_id_client_id FOREIGN KEY (client_id) REFERENCES public.client(id);
 R   ALTER TABLE ONLY public.purchase DROP CONSTRAINT fk_purchase_client_id_client_id;
       public          postgres    false    213    2766    205            �
           2606    43493 .   purchase fk_purchase_created_by_user_master_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.purchase
    ADD CONSTRAINT fk_purchase_created_by_user_master_id FOREIGN KEY (created_by) REFERENCES public.user_master(id);
 X   ALTER TABLE ONLY public.purchase DROP CONSTRAINT fk_purchase_created_by_user_master_id;
       public          postgres    false    213    2804    217                        2606    43498 ,   purchase fk_purchase_customer_id_customer_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.purchase
    ADD CONSTRAINT fk_purchase_customer_id_customer_id FOREIGN KEY (customer_id) REFERENCES public.customer(id);
 V   ALTER TABLE ONLY public.purchase DROP CONSTRAINT fk_purchase_customer_id_customer_id;
       public          postgres    false    2768    207    213                       2606    43508 3   purchase_items fk_purchase_item_client_id_client_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.purchase_items
    ADD CONSTRAINT fk_purchase_item_client_id_client_id FOREIGN KEY (client_id) REFERENCES public.client(id);
 ]   ALTER TABLE ONLY public.purchase_items DROP CONSTRAINT fk_purchase_item_client_id_client_id;
       public          postgres    false    205    215    2766                       2606    43513 +   purchase_items fk_purchase_items_product_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.purchase_items
    ADD CONSTRAINT fk_purchase_items_product_id FOREIGN KEY (product_id) REFERENCES public.product(id);
 U   ALTER TABLE ONLY public.purchase_items DROP CONSTRAINT fk_purchase_items_product_id;
       public          postgres    false    2786    215    211                       2606    43518 ,   purchase_items fk_purchase_items_purchase_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.purchase_items
    ADD CONSTRAINT fk_purchase_items_purchase_id FOREIGN KEY (purchase_id) REFERENCES public.purchase(id);
 V   ALTER TABLE ONLY public.purchase_items DROP CONSTRAINT fk_purchase_items_purchase_id;
       public          postgres    false    213    215    2794                       2606    43503 .   purchase fk_purchase_updated_by_user_master_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.purchase
    ADD CONSTRAINT fk_purchase_updated_by_user_master_id FOREIGN KEY (updated_by) REFERENCES public.user_master(id);
 X   ALTER TABLE ONLY public.purchase DROP CONSTRAINT fk_purchase_updated_by_user_master_id;
       public          postgres    false    217    2804    213                       2606    43523 .   user_master fk_user_master_client_id_client_id    FK CONSTRAINT     �   ALTER TABLE ONLY public.user_master
    ADD CONSTRAINT fk_user_master_client_id_client_id FOREIGN KEY (client_id) REFERENCES public.client(id);
 X   ALTER TABLE ONLY public.user_master DROP CONSTRAINT fk_user_master_client_id_client_id;
       public          postgres    false    2766    217    205            �   r   x��̱1�ڙ�zt��m�s:`�k+��P�(GD��� �]t��im�f�8B7�fB��gS:���oA(�2��:�X�����`�Q]2��V�d{�Wʚ�yJ_����ɥ�/�a@7      �   I   x�3���4202�50�50S04�2��26�372674�60�26�,I�-pH�M���K��sA�W� Pq�      �   �   x���I
�@��u�)��45t��R���0z	8�C �~�_ ZD`d��j��L!;���A͂�v��9�;�^���fOm@,N}��kPѴ�8f��=6\|N8s��1&��}x���Bq�e��1�T�e�_x7˫Ee%ݭ��~�Ƙ�k�      �      x������ � �      �   )  x���=n�0�g�ދ����-�	��{���-�AdKI:�h�}z$��0XO��L>�d�UD��sy7���Ϗ���,�yM�/�6K�R��|uY�"�t�KKe@mX.��W�FP%I�u��
P��1��&+8�`�E�]�l���j�M�����u��|g�>ym&�8���_%�[p��Rmv:Tƌ��R�>���˝�-#�P�Z��6���DR�vT��T���T���#�DKI(���oP�P-PA�J�F� �Ip�T�c�[NW��W{t�Wg{��4�W���      �   R   x�����0���(��NH�hRRe�9@TtHW\q���DM<6�aֲ{�]w�p�W"���R�`~��bp(ք��"rty,      �   J   x�E���0�f��҈]�������:����y�E4}) ���������Q��K�˾\_��g����	1�      �     x���Is�@ �3��r�j�7�iN�T�("V�,�F!.�¯���J��\_�ë�aC�� dw���TGP�?�f��ٶ��Zm�b����� E��:饅_�N��a�
�-l��y��)�"-�����p������v���2l�ڛ���UXlg[	�l�2��8����Ɠ�m�A��G��O����y�x(w;>�_h5���v!������I����g�����S�q|��`�ݮ�v���bU��z��#:����rx6��x2Z����Qow��D�e*��B4�$:��ˌ�L��H��ND�n jP��cF��lz��
n��%nP]EH��'|����|�R�ͭ˸�O��1��y�=�g�����"�W<޵N�~���5]�+b� �Yt��##�>���qL�g'=%�Q��K�rBo�{��L/�7~�+���ɾ�j��NB�;�.A��F�zu��6$����s�d��@$:T	H�@�r�
IY&��.�Ј�0:���m���V���}��     