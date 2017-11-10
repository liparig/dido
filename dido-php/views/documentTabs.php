<?php 
if(!count($list[Application_DocumentBrowser::LABEL_MD])){
?>
<div class="alert alert-danger">Nessuno.</div>
<?php
return;
} 
?>
<div class="panel-body">
	<ul class="nav nav-tabs">
<?php 
$XmlParser = new XMLParser();
foreach($list[Application_DocumentBrowser::LABEL_MD] as $sezione => $nomeDocumento):
	$badgeValue = ArrayHelper::countItems($list[Application_DocumentBrowser::LABEL_MD], $sezione);
	$sezione_field = Common::fieldFromLabel($sezione);
?>
	<!-- Nav tabs -->
		<li class="">
			<a href="<?="#".$sezione_field?>" data-toggle="tab" aria-expanded="false"><?=ucfirst($sezione)."&nbsp;&nbsp;&nbsp;<span class=\"badge\">$badgeValue</span>"?></a>
		</li>
<?php 
endforeach; 
?>
	</ul>
	<!-- Tab panes -->
	<div class="tab-content">
<?php 
foreach($list[Application_DocumentBrowser::LABEL_MD] as $sezione => $nomeDocumento): 
	$sezione_field = Common::fieldFromLabel($sezione);
?>
		<div class="tab-pane" id="<?=$sezione_field?>">
			<div class="panel-body">
				<ul class="nav nav-tabs">
<?php 
	foreach($nomeDocumento as $tipoDocumento=>$items):
		$tipoDocumento_field = Common::fieldFromLabel($tipoDocumento);
		$badgeValue = ArrayHelper::countItems($nomeDocumento, $tipoDocumento);
	
?>
				<!-- Nav tabs -->
					<li class="">
						<a href="<?="#".$sezione_field."/#".$tipoDocumento_field?>" data-toggle="tab" aria-expanded="false"><?=ucFirst($tipoDocumento)."&nbsp;&nbsp;&nbsp;<span class=\"badge\">$badgeValue</span>"?></a>
					</li>
<?php 
	endforeach;
?>
				</ul>
				<!-- Tab panes -->
				<div class="tab-content">
<?php 
	foreach($nomeDocumento as $tipoDocumento=>$versioneXML):
	
		$xmlToBeParsed = $XMLDataSource->getSingleXmlByFilename(key($versioneXML));
		$tipoDocumento_field = Common::fieldFromLabel($tipoDocumento);
		$XmlParser->setXMLSource( $xmlToBeParsed[XMLDataSource::LABEL_XML]);
		$inputs = $XmlParser->getMasterDocumentInputs();
?>
					<div class="tab-pane" id="<?=$tipoDocumento_field?>">
<?php 
		foreach($versioneXML as $items){
?>
						<table class="table table-condensed table-striped">
							<thead>
							<tr>
<?php 
			foreach($inputs as $input):
				if(isset($input[XMLParser::SHORTWIEW])):
?>
								<th><?=Common::labelFromField((string)$input);?></th>
<?php 
				endif;
?>
<?php 
			endforeach;
?>
								<th>
								</th>
							</tr>
							</thead>		
<?php 
			foreach($items as $k=>$item):
				$formId = $tipoDocumento_field.$k;
?>
							<tr>
<?php 
				$obj = $list[Application_DocumentBrowser::LABEL_MD_DATA][$k];
				foreach($inputs as $input):
					if(isset($input[XMLParser::SHORTWIEW])):
						$key = Common::labelFromField((string)$input, false);
						$value= Common::renderValue($obj[$key],$input);
?>
								<td>
									<?=$value?>
								</td>
<?php 
					endif;
				endforeach;			
?>
								<td class="text-right">
									<a class="btn btn-primary detail"
							href="?<?=Masterdocument::ID_MD?>=<?=$k?>"><span class="fa fa-search fa-1x fa-fw"></span>
								Dettaglio</a>
									
								</td>
							</tr>
<?php		
			endforeach;
?>					
						</table>
					</div>
<?php 
		}
		endforeach;
		
?>
				</div>		
			</div>
		</div>
<?php 
endforeach;
?>
	</div>
</div>