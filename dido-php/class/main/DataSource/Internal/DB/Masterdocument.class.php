<?php

class Masterdocument extends Crud{

	const ID_MD = "id_md";

	const TYPE = "type";

	const NOME = SharedDocumentConstants::NOME;

	const CLOSED = SharedDocumentConstants::CLOSED;

	const XML = "xml";

	const FTP_FOLDER = "ftp_folder";

	protected $TABLE = "master_documents";

	protected $SEQ_NAME = "master_documents_id_md_seq";

	public function __construct($connInstance) {
		parent::__construct ( $connInstance );
	}
}

?>