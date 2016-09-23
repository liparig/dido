package dido.signature;


import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TimeStampToken;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.AcroFields.FieldPosition;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.security.CertificateInfo;
import com.itextpdf.text.pdf.security.PdfPKCS7;
import com.itextpdf.text.pdf.security.SignaturePermissions;
import com.itextpdf.text.pdf.security.SignaturePermissions.FieldLock;

import dido.signature.interfaces.iSignatureManager;

public class SignatureManager implements iSignatureManager {
	final static Logger logger = Logger.getLogger(SignatureManager.class);
	private String pdfPath = null;
	private List<Signature> signatures = null;
	private Signature tmpSignature = null;
	public SignatureManager(String pdfPath){
		super();
		loadPDF(pdfPath);

	}

	private PdfPKCS7 verifySignature(AcroFields fields, String name) throws GeneralSecurityException, IOException {
		logger.info("Signature covers whole document: " + fields.signatureCoversWholeDocument(name));
		this.tmpSignature.setName(name);

		logger.info("Document revision: " + fields.getRevision(name) + " of " + fields.getTotalRevisions());
		this.tmpSignature.setRevision(fields.getRevision(name));
		this.tmpSignature.setTotalREvisions(fields.getTotalRevisions());

		PdfPKCS7 pkcs7 = fields.verifySignature(name);
		logger.info("Integrity check OK? " + pkcs7.verify());
		this.tmpSignature.setComplete(pkcs7.verify());

		return pkcs7;
	}

	private SignaturePermissions inspectSignature(AcroFields fields, String name, SignaturePermissions perms) throws GeneralSecurityException, IOException {
		List<FieldPosition> fps = fields.getFieldPositions(name);
		if (fps != null && fps.size() > 0) {
			FieldPosition fp = fps.get(0);
			Rectangle pos = fp.position;
			if (pos.getWidth() == 0 || pos.getHeight() == 0) {
				logger.info("Invisible signature");
				this.tmpSignature.setInvisible(true);
			}
			else {
				this.tmpSignature.setInvisible(false);
				logger.info(String.format("Field on page %s; llx: %s, lly: %s, urx: %s; ury: %s",
						fp.page, pos.getLeft(), pos.getBottom(), pos.getRight(), pos.getTop()));
			}
		}

		PdfPKCS7 pkcs7 = this.verifySignature(fields, name);

		logger.info("Digest algorithm: " + pkcs7.getHashAlgorithm());
		this.tmpSignature.setHashAlgorithm(pkcs7.getHashAlgorithm());

		logger.info("Encryption algorithm: " + pkcs7.getEncryptionAlgorithm());
		this.tmpSignature.setEncryptionAlgorithm(pkcs7.getEncryptionAlgorithm());

		logger.info("Filter subtype: " + pkcs7.getFilterSubtype());
		if(pkcs7.getFilterSubtype()!=null)
		this.tmpSignature.setFilterSubtype(pkcs7.getFilterSubtype().toString());

		X509Certificate cert = (X509Certificate) pkcs7.getSigningCertificate();
		logger.info("Name of the signer: " + CertificateInfo.getSubjectFields(cert).getField("CN"));
		this.tmpSignature.setSigner(CertificateInfo.getSubjectFields(cert).getField("CN"));

		BCRSAPublicKey pub = (BCRSAPublicKey) cert.getPublicKey();
		logger.info("Public Key: " + pub.getModulus().toString(16));
		this.tmpSignature.setPublicKey(pub.getModulus().toString(16));

		if (pkcs7.getSignName() != null){
			logger.info("Alternative name of the signer: " + pkcs7.getSignName());
			this.tmpSignature.setSignerAlt(pkcs7.getSignName());
		} else {
			this.tmpSignature.setSignerAlt(null);
		}

		SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
		logger.info("Signed on: " + date_format.format(pkcs7.getSignDate().getTime()));
		this.tmpSignature.setSignDate(date_format.format(pkcs7.getSignDate().getTime()));

		if (pkcs7.getTimeStampDate() != null) {
			logger.info("TimeStamp: " + date_format.format(pkcs7.getTimeStampDate().getTime()));
			TimeStampToken ts = pkcs7.getTimeStampToken();
			logger.info("TimeStamp service: " + ts.getTimeStampInfo().getTsa());
			logger.info("Timestamp verified? " + pkcs7.verifyTimestampImprint());
		}
		logger.info("Location: " + pkcs7.getLocation());
		logger.info("Reason: " + pkcs7.getReason());
		PdfDictionary sigDict = fields.getSignatureDictionary(name);
		PdfString contact = sigDict.getAsString(PdfName.CONTACTINFO);
		if (contact != null)
			logger.info("Contact info: " + contact);
		perms = new SignaturePermissions(sigDict, perms);

		logger.info("Signature type: " + (perms.isCertification() ? "certification" : "approval"));
		this.tmpSignature.setSignType(perms.isCertification() ? "certification" : "approval");

		logger.info("Filling out fields allowed: " + perms.isFillInAllowed());
		logger.info("Adding annotations allowed: " + perms.isAnnotationsAllowed());
		for (FieldLock lock : perms.getFieldLocks()) {
			logger.info("Lock: " + lock.toString());
		}
		return perms;
	}

	private void inspectSignatures(String path) throws IOException, GeneralSecurityException {
		logger.info("Path document to inspect"+path);
		PdfReader reader = new PdfReader(path);
		AcroFields fields = reader.getAcroFields();
		ArrayList<String> names = fields.getSignatureNames();
		SignaturePermissions perms = null;
		for (String name : names) {
			this.tmpSignature=new Signature();
			logger.info("===== " + name + " =====");
			perms = inspectSignature(fields, name, perms);
			signatures.add(tmpSignature);
		}
		logger.info("end ispection");

	}

	public boolean loadPDF(String path){
		signatures=new ArrayList<Signature>();
		pdfPath=path;
		BouncyCastleProvider provider = new BouncyCastleProvider();
		Security.addProvider(provider);
		try {
			this.inspectSignatures(this.pdfPath);
		} catch (IOException e) {
			e.printStackTrace();
	        logger.error("Could not find file:" + pdfPath);

			return false;
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
	        logger.error("Could not find file:" + pdfPath);
	        return false;
		}
		return true;
	}

	public List<Signature> getSignatures() {
		return this.signatures;
	}
}