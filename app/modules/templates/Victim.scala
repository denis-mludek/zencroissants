package modules.mail.templates

object Victim {

  def template(victim: String) = {
    s"""<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <html xmlns="http://www.w3.org/1999/xhtml"> <head> <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href='https://fonts.googleapis.com/css?family=Montserrat:400,700' rel='stylesheet' type='text/css'> </head>
    <style> body {max-width: 400px; font-family: "Montserrat"; color: #4A4A4A;} table {font-size: 18px; }
    table.layout { width: 600px; padding: 0 100px 100px 100px;}
    table.troll { color: #50E3C2; font-size: 28px; margin: 30px 0; font-weight: bold;}
    table.info { width: 600px; height: 100px; background-color: #E35C4F; font-size: 28px; padding-left: 80px; color: white;}
    table.title {font-size: 22px; } a {text-decoration: none; color: white; font-weight: bold; }
    .btn {width: 100%; padding: 20px; margin: 20px 0; border-radius: 5px; text-align: center; box-sizing: border-box;}
    table.layout table {margin: 20px 0; width: 100%;} .valid {background-color: #50E3C2; } .error {background-color: #E35C4F; }
    .rules-title { color: black; font-size: 20px; font-weight: bold; } .rules-description { font-size: 16px; } </style>
     </head> <body> <table class="layout"> <tr> <td> <table class="title"> <tr> <td>Salut ${victim},</td> </tr> </table>
      <table> <tr> <td> Il semble que tu te sois fait avoir... tu sais ce qu'il te reste à faire. </td> </tr> </table>
       <table class="troll"> <tr> <td> PAYER TES CROISSANTS </td> </tr> </table> <table> <tr>
        <td> Attention tu seras jugé sur plusieurs critères : </td> </tr> </table> <table> <tr class="rules-title">
        <td> 1. Le respect de ton engagement </td> </tr> <tr class="rules-description">
         <td> Tout manquement au règlement entrainera un durcissement de la sentence et beaucoup de trolls. </td> </tr> </table>
         <table> <tr class="rules-title"> <td> 2. La qualité des croissants </td> </tr> <tr class="rules-description">
          <td> (ou tout autre collation accompagnant les thés ou cafés des Zengular). </td> </tr> </table>
           <table> <tr class="rules-title"> <td> 3. La quantité des croissants </td> </tr> <tr class="rules-description">
           <td> Quoi de pire qu’un Zengular sans croissant ? Un Zengular qui pensait en avoir mais qui finalement n’en a pas. </td>
           </tr> </table> </td> </tr> </table> <table class="info"> <tr> <td> Bon Chance ! </td> </tr> </table> </body> </html>"""
  }
}
